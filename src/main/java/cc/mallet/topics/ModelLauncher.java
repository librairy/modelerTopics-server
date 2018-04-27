package cc.mallet.topics;

import com.google.common.base.Strings;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.librairy.service.modeler.facade.model.Dimension;
import org.librairy.service.modeler.facade.model.Element;
import org.librairy.service.modeler.facade.model.Model;
import org.librairy.service.modeler.service.StatsService;
import org.librairy.service.modeler.service.TimeService;
import org.librairy.service.modeler.service.TopicsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ModelLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(ModelLauncher.class);

    DatumWriter<Model> modelDatumWriter = new SpecificDatumWriter<Model>(Model.class);

    DatumReader<Model> modelDatumReader = new SpecificDatumReader<Model>(Model.class);

    @Autowired
    TopicsService topicsService;

    public boolean existsModel(String baseDir){
        return Paths.get(baseDir,"model-inferencer.bin").toFile().exists();
    }

    public boolean removeModel(String baseDir){
        try{
            Paths.get(baseDir,"model-inferencer.bin").toFile().delete();
            Paths.get(baseDir,"model-stats.bin").toFile().delete();
            Paths.get(baseDir,"model-parameters.bin").toFile().delete();
            Paths.get(baseDir,"model-topics.csv.gz").toFile().delete();
            Paths.get(baseDir,"model-topic-words.csv.gz").toFile().delete();
            return true;
        }catch (Exception e){
            LOG.warn("Error deleting models",e);
            return false;
        }
    }

    public TopicInferencer getTopicInferencer(String baseDir) throws Exception {
        return TopicInferencer.read(Paths.get(baseDir,"model-inferencer.bin").toFile());
    }

    public Model getDetails(String baseDir) throws Exception {

        DataFileReader<Model> dataFileReader = new DataFileReader<Model>(Paths.get(baseDir,"model-stats.bin").toFile(), modelDatumReader);
        Model model = dataFileReader.next();
        dataFileReader.close();
        return model;

    }

    public void saveModel(String baseDir, String algorithm, ModelParams parameters, ParallelTopicModel model, Integer numTopWords) throws IOException {
        try {
            File modelFolder = new File(baseDir);
            if (!modelFolder.exists()) modelFolder.mkdirs();

            LOG.info("saving model params..");
            Map<String,String> params = new HashMap<>();
            params.put("part-of-speech",parameters.getPos());
            params.put("language",parameters.getLanguage());
            params.put("alpha",String.valueOf(parameters.getAlpha()));
            params.put("beta",String.valueOf(parameters.getBeta()));
            params.put("topics",String.valueOf(parameters.getNumTopics()));
            params.put("iterations",String.valueOf(parameters.getNumIterations()));
            params.put("optimization",String.valueOf(parameters.getNumRetries()));
            params.put("top-words",String.valueOf(parameters.getNumTopWords()));

            LOG.info("saving model stats..");
            TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(model, numTopWords);

            Map<String,String> stats = new HashMap<>();
            stats.put("loglikelihood", String.valueOf(model.modelLogLikelihood()));
            stats.put("vocabulary", String.valueOf(model.alphabet.size()));
            stats.put("doc-lengths", StatsService.from(model.docLengthCounts));
            stats.put("num-topics", String.valueOf(model.getNumTopics()));
            stats.put("stopwords", model.stoplist.toString());
            stats.put("topic-coherence", StatsService.from(diagnostics.getCoherence().scores));
            stats.put("topic-distance", StatsService.from(diagnostics.getDistanceFromCorpus().scores));
            stats.put("alpha-sum", String.valueOf(model.alphaSum));
            stats.put("beta-sum", String.valueOf(model.betaSum));

            Model modelDetails = Model.newBuilder().setAlgorithm(algorithm).setDate(TimeService.now()).setParams(params).setStats(stats).build();
            DataFileWriter<Model> dataFileWriter = new DataFileWriter<Model>(modelDatumWriter);
            dataFileWriter.create(modelDetails.getSchema(),Paths.get(baseDir,"model-stats.bin").toFile());
            dataFileWriter.append(modelDetails);
            dataFileWriter.close();

            LOG.info("saving model topics..");
            Map<Integer, List<Element>> topWords = topicsService.getTopWords(model, numTopWords);

            // save topic words
            List<String> topicWords = topWords.entrySet().parallelStream().flatMap(entry -> entry.getValue().stream().map(word -> entry.getKey() + ";;" + word.getValue() + ";;" + word.getScore() +"\n")).collect(Collectors.toList());
            saveToFile(topicWords, Paths.get(baseDir, "model-topic-words.csv.gz"));

            // save topics
            List<String> topics = IntStream.range(0, topWords.size()).parallel().mapToObj(i -> i + ";;" + model.getTopicAlphabet().lookupObject(i) + ";;" + topWords.get(i).stream().limit(10).map(w -> w.getValue()).collect(Collectors.joining(","))+"\n").collect(Collectors.toList());
            saveToFile(topics, Paths.get(baseDir, "model-topics.csv.gz"));

            LOG.info("saving model inferencer..");
            ObjectOutputStream e2 = new ObjectOutputStream(new FileOutputStream(Paths.get(baseDir, "model-inferencer.bin").toFile()));
            e2.writeObject(model.getInferencer());
            e2.close();

            LOG.info("saving topics statistics..");
            saveParameters(baseDir, parameters);
            LOG.info("model saved successfully");

            topicsService.loadModel();

        } catch (Exception var6) {
            LOG.warn("Couldn\'t save model: " + var6.getMessage());
        }


    }

    private void saveToFile(List<String> lines, Path filePath) throws IOException {

        File outputFile = filePath.toFile();
        if (outputFile.exists()) outputFile.delete();
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(outputFile, false))));

        lines.forEach(line -> {
            try {
                writer.write(line);
            } catch (IOException e) {
                LOG.warn("Error writing in file: " + filePath, e);
            }
        });

        writer.close();
    }

    private List<String> readFromFile(Path filePath) throws IOException {

        BufferedReader reader = new BufferedReader(new InputStreamReader(new GZIPInputStream(new FileInputStream(filePath.toFile()))));
        String line = null;
        List<String> lines = new ArrayList<>();
        while(!Strings.isNullOrEmpty(line = reader.readLine())){
            lines.add(line);
        }
        reader.close();

        return lines;
    }

    public List<Dimension> readTopics(String baseDir) throws IOException {

        return readFromFile(Paths.get(baseDir,"model-topics.csv.gz")).parallelStream().map(line -> {
            String[] values = line.split(";;");

            Dimension dimension = new Dimension();
            dimension.setId(Integer.valueOf(values[0]));
            dimension.setName(values[1]);
            dimension.setDescription(values[2]);
            return  dimension;
        }).collect(Collectors.toList());
    }

    public Map<Integer, List<Element>> readTopicWords(String baseDir) throws IOException {

        ConcurrentHashMap<Integer,List<Element>> topicWords = new ConcurrentHashMap();

        readFromFile(Paths.get(baseDir, "model-topic-words.csv.gz")).parallelStream().forEach( line -> {
            String[] values = line.split(";;");

            Integer id = Integer.valueOf(values[0]);
            Element word = new Element();
            word.setValue(values[1]);
            word.setScore(Double.valueOf(values[2]));

            if (!topicWords.containsKey(id)){
                topicWords.put(id,new ArrayList<>());
            }

            topicWords.get(id).add(word);

        });

        return topicWords;
    }

    public void saveParameters(String baseDir, ModelParams parameters) throws IOException {
        FileOutputStream fout = new FileOutputStream(Paths.get(baseDir,"model-parameters.bin").toFile());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(parameters);
        oos.close();
        fout.close();
    }

    public ModelParams readParameters(String baseDir) throws IOException, ClassNotFoundException {
        FileInputStream fout = new FileInputStream(Paths.get(baseDir,"model-parameters.bin").toFile());
        ObjectInputStream oos = new ObjectInputStream(fout);
        ModelParams parameters = (ModelParams) oos.readObject();
        oos.close();
        fout.close();
        return parameters;
    }

}

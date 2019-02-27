package cc.mallet.topics;

import cc.mallet.pipe.Pipe;
import cc.mallet.types.Alphabet;
import cc.mallet.types.InstanceList;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.base.Strings;
import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.librairy.service.modeler.facade.model.*;
import org.librairy.service.modeler.service.InferencePoolManager;
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
import java.util.*;
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

    DatumWriter<Settings> modelDatumWriter = new SpecificDatumWriter<Settings>(Settings.class);

    DatumReader<Settings> modelDatumReader = new SpecificDatumReader<Settings>(Settings.class);

    ObjectMapper jsonMapper = new ObjectMapper();

    @Autowired
    TopicsService topicsService;

    @Autowired
    InferencePoolManager inferencePoolManager;

    public boolean existsModel(String baseDir){
        return Paths.get(baseDir,"model-inferencer.bin").toFile().exists();
    }

    public boolean removeModel(String baseDir){
        try{
            Paths.get(baseDir,"model-inferencer.bin").toFile().delete();
            Paths.get(baseDir,"model-settings.bin").toFile().delete();
            Paths.get(baseDir,"model-parameters.bin").toFile().delete();
            Paths.get(baseDir,"model-topics.csv.gz").toFile().delete();
            Paths.get(baseDir,"model-topic-words.csv.gz").toFile().delete();
            Paths.get(baseDir,"model-alphabet.bin").toFile().delete();
            Paths.get(baseDir,"model-pipe.bin").toFile().delete();
            return true;
        }catch (Exception e){
            LOG.warn("Error deleting models",e);
            return false;
        }
    }

    public TopicInferencer getTopicInferencer(String baseDir) throws Exception {
        return TopicInferencer.read(Paths.get(baseDir,"model-inferencer.bin").toFile());
    }

    public Settings getDetails(String baseDir) throws Exception {

        DataFileReader<Settings> dataFileReader = new DataFileReader<Settings>(Paths.get(baseDir,"model-settings.bin").toFile(), modelDatumReader);
        Settings model = dataFileReader.next();
        dataFileReader.close();
        return model;

    }

    public void saveModel(String baseDir, String algorithm, ModelParams parameters, ParallelTopicModel model, Integer numTopWords, Pipe pipe) throws IOException {
        try {
            File modelFolder = new File(baseDir);
            if (!modelFolder.exists()) modelFolder.mkdirs();


            LOG.info("saving doctopics .. ");
            File docTopicsFile = Paths.get(baseDir, "doctopics.csv.gz").toFile();
            if (docTopicsFile.exists()) docTopicsFile.delete();
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(docTopicsFile, true))));
            Map<Integer, Map<Integer, Integer>> concurrenceTopicMap = model.printDenseDocumentTopicsAsCSV(new PrintWriter(writer));
            writer.close();
            LOG.info("doctopics file created at: " + docTopicsFile.getAbsolutePath());


            LOG.info("saving topic neighbours by concurrence.. ");
            File coTopicsFile = Paths.get(baseDir, "model-topic-neighbours.csv.gz").toFile();
            if (coTopicsFile.exists()) coTopicsFile.delete();
            BufferedWriter w2 = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(coTopicsFile, true))));
            for (Integer topic : concurrenceTopicMap.keySet()){
                Map<Integer, Integer> related = concurrenceTopicMap.get(topic);
                for(Integer relTopic : related.keySet()){
                    Integer freq = related.get(relTopic);
                    Integer maxFreq = model.getData().size();
                    Double score = Double.valueOf(freq) / Double.valueOf(maxFreq);
                    w2.write(topic+";;"+relTopic+";;"+score+";;"+Similarity.CONCURRENCE.name()+"\n");
                }
            }
            w2.close();
            LOG.info("topic neighbours file created at: " + coTopicsFile.getAbsolutePath());


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
            params.put("min-freq",String.valueOf(parameters.getMinFreq()));
            params.put("max-doc-ratio",String.valueOf(parameters.getMaxDocRatio()));
            params.put("stop-words",parameters.getStopwords().toString());
            params.put("entities",parameters.getEntities().toString());
            params.put("seed",parameters.getSeed().toString());

            LOG.info("saving model stats..");
            TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(model, numTopWords<0?50:numTopWords);


            Map<String,String> stats = new HashMap<>();
            stats.put("loglikelihood", String.valueOf(model.modelLogLikelihood()));
            stats.put("vocabulary", String.valueOf(model.alphabet.size()));
            stats.put("corpus", String.valueOf(model.getData().size()));
            stats.put("model-stop-words", model.stoplist.size() > 100? model.stoplist.subList(0,100).toString() : model.stoplist.toString());
            stats.put("topic-coherence", StatsService.from(diagnostics.getCoherence().scores));
            stats.put("topic-distance", StatsService.from(diagnostics.getDistanceFromCorpus().scores));
            stats.put("alpha-sum", String.valueOf(model.alphaSum));
            stats.put("beta-sum", String.valueOf(model.betaSum));
            stats.put("alpha-topics", Arrays.asList(model.alpha).stream().map(d -> String.valueOf(d)).collect(Collectors.joining(", ")));

            Settings modelDetails = Settings.newBuilder().setAlgorithm(algorithm).setDate(TimeService.now()).setParams(params).setStats(stats).build();
            DataFileWriter<Settings> dataFileWriter = new DataFileWriter<Settings>(modelDatumWriter);
            dataFileWriter.create(modelDetails.getSchema(),Paths.get(baseDir,"model-settings.bin").toFile());
            dataFileWriter.append(modelDetails);
            dataFileWriter.close();

            LOG.info("saving model topics..");
            Map<Integer, List<TopicWord>> topWords = topicsService.getTopWords(model, numTopWords);

            // calculate entropy for each topic
            Map<Integer,Double> entropies = new HashMap<>();

            for(Integer topicId : topWords.keySet()){
                List<TopicWord> tw = topWords.get(topicId);
                Double entropy = ((tw != null) && (!tw.isEmpty()))? StatsService.entropy(topWords.get(topicId).stream().map(el -> el.getScore()).collect(Collectors.toList())) : 0.0;
                entropies.put(topicId,entropy);
            }

            // save topic words
            List<String> topicWords = topWords.entrySet().parallelStream().flatMap(entry -> entry.getValue().stream().map(word -> entry.getKey() + ";;" + word.getValue().replace(";","") + ";;" + word.getScore() +"\n")).collect(Collectors.toList());
            saveToFile(topicWords, Paths.get(baseDir, "model-topic-words.csv.gz"));

            // save topics
            List<String> topics = IntStream.range(0, topWords.size()).parallel().mapToObj(i -> i + ";;" + model.getTopicAlphabet().lookupObject(i) + ";;" + topWords.get(i).stream().sorted( (a,b) -> -a.getScore().compareTo(b.getScore())).limit(10).map(w -> w.getValue().replace(";","")).collect(Collectors.joining(","))+ ";;" +  entropies.get(i) + "\n").collect(Collectors.toList());
            saveToFile(topics, Paths.get(baseDir, "model-topics.csv.gz"));


            LOG.info("saving model parameters..");
            saveParameters(baseDir, parameters);

            LOG.info("saving model inferencer..");
            ObjectOutputStream e2 = new ObjectOutputStream(new FileOutputStream(Paths.get(baseDir, "model-inferencer.bin").toFile()));
            TopicInferencer inferencer = model.getInferencer();
            inferencer.setRandomSeed(parameters.getSeed());
            e2.writeObject(inferencer);
            e2.close();

            LOG.info("saving model alphabet..");
            ObjectOutputStream e3 = new ObjectOutputStream(new FileOutputStream(Paths.get(baseDir, "model-alphabet.bin").toFile()));
            //e3.writeObject(model.getAlphabet());
            e3.writeObject(pipe.getAlphabet());
            e3.close();

            LOG.info("saving model pipe..");
            ObjectOutputStream e4 = new ObjectOutputStream(new FileOutputStream(Paths.get(baseDir, "model-pipe.bin").toFile()));
            e4.writeObject(pipe);
            e4.close();


            LOG.info("model saved successfully");

            topicsService.loadModel();

            inferencePoolManager.update(pipe.getAlphabet(), pipe);

        } catch (Exception var6) {
            LOG.warn("Couldn\'t save model", var6);
        }

    }


    public Alphabet readModelAlphabet(String baseDir) throws IOException, ClassNotFoundException {
        LOG.info("reading model-alphabet..");
        ObjectInputStream e3 = new ObjectInputStream(new FileInputStream(Paths.get(baseDir, "model-alphabet.bin").toFile()));
        Alphabet alphabet = (Alphabet) e3.readObject();
        e3.close();
        LOG.info("model-alphabet read!");
        return alphabet;
    }

    public Pipe readModelPipe(String baseDir) throws IOException, ClassNotFoundException {
        LOG.info("reading model-pipe..");
        ObjectInputStream e3 = new ObjectInputStream(new FileInputStream(Paths.get(baseDir, "model-pipe.bin").toFile()));
        Pipe pipe = (Pipe) e3.readObject();
        e3.close();
        LOG.info("model-pipe read");
        return pipe;
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

    public Map<Integer,Topic> readTopics(String baseDir) throws IOException {

        Map<Integer,Topic> topics = new ConcurrentHashMap<>();


        readFromFile(Paths.get(baseDir,"model-topics.csv.gz")).parallelStream().forEach(line -> {
            String[] values = line.split(";;");

            Integer id          = Integer.valueOf(values[0]);
            String name         = values[1];
            String description  = values[2];
            Double entropy      = values.length > 3? Double.valueOf(values[3]) : 0.0;

            topics.put(id,Topic.newBuilder().setId(id).setName(name).setDescription(description).setEntropy(entropy).build());


        });

        return topics;
    }

    public Map<Integer, List<TopicWord>> readTopicWords(String baseDir) throws IOException {

        ConcurrentHashMap<Integer,List<TopicWord>> topicWords = new ConcurrentHashMap();

        readFromFile(Paths.get(baseDir, "model-topic-words.csv.gz")).stream().forEach( line -> {
            String[] values = line.split(";;");

            Integer id = Integer.valueOf(values[0]);
            TopicWord word = new TopicWord();
            word.setValue(values[1]);
            word.setScore(Double.valueOf(values[2]));

            if (!topicWords.containsKey(id)){
                topicWords.put(id,new ArrayList<>());
            }

            topicWords.get(id).add(word);

        });

        // Sort topic words
        LOG.info("sorting topic words..");
        topicWords.keySet().parallelStream().forEach(key -> topicWords.put(key, topicWords.get(key).stream().sorted((a,b) -> -a.getScore().compareTo(b.getScore())).collect(Collectors.toList())));

        topicWords.entrySet().forEach(entry -> LOG.info("Topic " + entry.getKey() + " : " + entry.getValue().size() + " words"));

        return topicWords;
    }


    public Map<Integer, List<TopicNeighbour>> readTopicNeighbours(String baseDir) throws IOException {

        ConcurrentHashMap<Integer,List<TopicNeighbour>> topicNeighbours = new ConcurrentHashMap();

        readFromFile(Paths.get(baseDir, "model-topic-neighbours.csv.gz")).stream().forEach( line -> {
            String[] values = line.split(";;");

            Integer topicId         = Integer.valueOf(values[0]);

            Integer neighbourId     = Integer.valueOf(values[1]);

            Double score            = Double.valueOf(values[2]);

            Similarity similarity   = Similarity.valueOf(values[3].toUpperCase());

            TopicNeighbour neighbour = TopicNeighbour.newBuilder().setId(neighbourId).setScore(score).setSimilarity(similarity).build();

            if (!topicNeighbours.containsKey(topicId)){
                topicNeighbours.put(topicId,new ArrayList<>());
            }

            topicNeighbours.get(topicId).add(neighbour);

        });

        // Sort topic words
        LOG.info("sorting topic neighbours..");
        topicNeighbours.keySet().parallelStream().forEach(key -> topicNeighbours.put(key, topicNeighbours.get(key).stream().sorted((a,b) -> -a.getScore().compareTo(b.getScore())).collect(Collectors.toList())));

        topicNeighbours.entrySet().forEach(entry -> LOG.info("Topic " + entry.getKey() + " : " + entry.getValue().size() + " neighbours"));

        return topicNeighbours;
    }


    public void saveParameters(String baseDir, ModelParams parameters) throws IOException {
        jsonMapper.writeValue(Paths.get(baseDir,"model-parameters.bin").toFile(), parameters);
    }

    public ModelParams readParameters(String baseDir) throws IOException, ClassNotFoundException {
        return jsonMapper.readValue(Paths.get(baseDir,"model-parameters.bin").toFile(), ModelParams.class);
    }

}

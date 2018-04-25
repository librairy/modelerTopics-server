package cc.mallet.topics;

import org.apache.avro.file.DataFileReader;
import org.apache.avro.file.DataFileWriter;
import org.apache.avro.io.DatumReader;
import org.apache.avro.io.DatumWriter;
import org.apache.avro.specific.SpecificDatumReader;
import org.apache.avro.specific.SpecificDatumWriter;
import org.librairy.service.modeler.facade.model.Model;
import org.librairy.service.modeler.service.StatsService;
import org.librairy.service.modeler.service.TimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ModelLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(ModelLauncher.class);

    DatumWriter<Model> modelDatumWriter = new SpecificDatumWriter<Model>(Model.class);

    DatumReader<Model> modelDatumReader = new SpecificDatumReader<Model>(Model.class);

    public boolean existsModel(String baseDir){
        return Paths.get(baseDir,"model-inferencer.bin").toFile().exists() && Paths.get(baseDir,"model-parallel.bin").toFile().exists();
    }

    public boolean removeModel(String baseDir){
        try{
            Paths.get(baseDir,"model-inferencer.bin").toFile().delete();
            Paths.get(baseDir,"model-parallel.bin").toFile().delete();
            Paths.get(baseDir,"model-details.bin").toFile().delete();
            return true;
        }catch (Exception e){
            LOG.warn("Error deleting models",e);
            return false;
        }
    }

    public TopicInferencer getTopicInferencer(String baseDir) throws Exception {
        return TopicInferencer.read(Paths.get(baseDir,"model-inferencer.bin").toFile());
    }

    public ParallelTopicModel getTopicModel(String baseDir) throws Exception {
        return ParallelTopicModel.read(Paths.get(baseDir,"model-parallel.bin").toFile());
    }

    public Model getDetails(String baseDir) throws Exception {

        DataFileReader<Model> dataFileReader = new DataFileReader<Model>(Paths.get(baseDir,"model-details.bin").toFile(), modelDatumReader);
        Model model = dataFileReader.next();
        dataFileReader.close();
        return model;

    }

    public void saveModel(String baseDir, String algorithm, ModelParams parameters, ParallelTopicModel model, Integer numTopWords) throws IOException {
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

        Model modelDetails = Model.newBuilder().setAlgorithm(algorithm).setDate(TimeService.now()).setParams(params).setStats(stats).build();
        DataFileWriter<Model> dataFileWriter = new DataFileWriter<Model>(modelDatumWriter);
        dataFileWriter.create(modelDetails.getSchema(),Paths.get(baseDir,"model-details.bin").toFile());
        dataFileWriter.append(modelDetails);
        dataFileWriter.close();

        LOG.info("saving model parallel..");
        model.write(Paths.get(baseDir, "model-parallel.bin").toFile());
        ObjectOutputStream e2;
        try {
            LOG.info("saving model inferencer..");
            e2 = new ObjectOutputStream(new FileOutputStream(Paths.get(baseDir, "model-inferencer.bin").toFile()));
            e2.writeObject(model.getInferencer());
            e2.close();
        } catch (Exception var6) {
            LOG.warn("Couldn\'t create inferencer: " + var6.getMessage());
        }
        LOG.info("saving topics statistics..");
        saveParameters(baseDir, parameters);
        LOG.info("model saved successfully");
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

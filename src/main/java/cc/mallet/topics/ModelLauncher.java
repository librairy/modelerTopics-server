package cc.mallet.topics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.file.Paths;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class ModelLauncher {

    private static final Logger LOG = LoggerFactory.getLogger(ModelLauncher.class);

    public boolean existsModel(String baseDir){
        return Paths.get(baseDir,"model-inferencer.bin").toFile().exists() && Paths.get(baseDir,"model-parallel.bin").toFile().exists();
    }

    public boolean removeModel(String baseDir){
        try{
            Paths.get(baseDir,"model-inferencer.bin").toFile().delete();
            Paths.get(baseDir,"model-parallel.bin").toFile().delete();
            Paths.get(baseDir,"model-parameters.bin").toFile().delete();
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

    public void saveModel(String baseDir, LDAParameters parameters, ParallelTopicModel model, Integer numTopWords) throws IOException {
        File modelFolder = new File(baseDir);
        if (!modelFolder.exists()) modelFolder.mkdirs();
        LOG.info("saving model parameters..");
        FileOutputStream fout = new FileOutputStream(Paths.get(baseDir,"model-parameters.bin").toFile());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(parameters);
        oos.close();
        fout.close();

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
        LOG.info("saving model statistics..");
        PrintWriter e1 = new PrintWriter(Paths.get(baseDir, "diagnostic.txt").toFile());
        TopicModelDiagnostics diagnostics = new TopicModelDiagnostics(model, numTopWords);
        e1.println(diagnostics.toXML());
        e1.close();
        saveParameters(baseDir, parameters);
        LOG.info("model saved successfully");
    }

    public void saveParameters(String baseDir, LDAParameters parameters) throws IOException {
        FileOutputStream fout = new FileOutputStream(Paths.get(baseDir,"model-parameters.bin").toFile());
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(parameters);
        oos.close();
        fout.close();
    }

    public LDAParameters readParameters(String baseDir) throws IOException, ClassNotFoundException {
        FileInputStream fout = new FileInputStream(Paths.get(baseDir,"model-parameters.bin").toFile());
        ObjectInputStream oos = new ObjectInputStream(fout);
        LDAParameters parameters = (LDAParameters) oos.readObject();
        oos.close();
        fout.close();
        return parameters;
    }

}

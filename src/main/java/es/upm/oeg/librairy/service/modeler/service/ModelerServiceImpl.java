package es.upm.oeg.librairy.service.modeler.service;

import org.apache.avro.AvroRemoteException;
import es.upm.oeg.librairy.service.modeler.builders.TopicSummaryBuilder;
import es.upm.oeg.librairy.service.modeler.facade.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class ModelerServiceImpl implements ModelerService {

    private static final Logger LOG = LoggerFactory.getLogger(ModelerServiceImpl.class);

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    String resourceFolder;

    String model              ;

    @Autowired
    TopicsService topicsService;

    @Autowired
    InferencePoolManager inferencePoolManager;

    private Map<Integer,TopicSummary> topicMap;

    @PostConstruct
    public void setup() throws IOException {

        //// Load resources
        //model              = Paths.get(resourceFolder,"resource.bin").toFile().getAbsolutePath();

        LOG.info("Service initialized");

        topicMap = new ConcurrentHashMap<>();
        List<TopicSummary> topics = getTopics();
        for(int i=0;i<topics.size();i++){
            topicMap.put(i,topics.get(i));
        }
    }

    @Override
    public Inference createInference(String text, boolean topics) throws AvroRemoteException {
        try {
            LOG.info("Creating inference from text..");
            List<Double> shape = inferencePoolManager.get(Thread.currentThread()).inference(text);
            if (!topics) return Inference.newBuilder().setVector(shape).build();

            TopicSummaryBuilder tsBuilder = new TopicSummaryBuilder(shape);


//            List<TopicWord> topTopics = IntStream.range(0, shape.size()).mapToObj(i -> TopicWord.newBuilder().setScore(shape.get(i)).setValue("" + i).build()).filter(w -> w.getScore() > (1.0 / Double.valueOf(shape.size()))).sorted((a, b) -> -a.getScore().compareTo(b.getScore())).collect(Collectors.toList());

            List<TopicSummary> topTopics = tsBuilder.getTopTopics().stream().map(i -> {
                Integer id = i.getId();
                i.setId(Integer.valueOf(i.getDescription().replace("level","")));

                TopicSummary ts = topicMap.get(id);
                i.setName(ts.getName());
                i.setDescription(ts.getDescription());
                return i;
            }).collect(Collectors.toList());

            return Inference.newBuilder().setVector(shape).setTopics(topTopics).build();
        } catch (Exception e) {
            throw new AvroRemoteException("Error loading topic model",e);
        }
    }

    @Override
    public List<TopicSummary> assignClasses(String s) throws AvroRemoteException {
        return createInference(s,true).getTopics();
    }

    @Override
    public List<TopicSummary> getTopics() throws AvroRemoteException {
        return topicsService.getTopics();
    }

    @Override
    public Topic getTopic(int id) throws AvroRemoteException {
        return topicsService.get(id);
    }

    @Override
    public List<TopicWord> getTopicWords(int topicId, int max, int offset, boolean tfidf) throws AvroRemoteException {
        return tfidf? topicsService.getWordsByTFIDF(topicId, max, offset) : topicsService.getWords(topicId,max,offset);
    }

    @Override
    public List<TopicNeighbour> getTopicNeighbours(int id, int max, Similarity similarity) throws AvroRemoteException {
        return topicsService.getNeighbours(id, max, similarity);
    }

    @Override
    public Settings getSettings() throws AvroRemoteException {
        return topicsService.getSettings();
    }
}

package org.librairy.service.modeler.service;

import io.swagger.models.auth.In;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.builders.TopicSummaryBuilder;
import org.librairy.service.modeler.facade.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

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

    @PostConstruct
    public void setup() throws IOException {

        //// Load resources
        //model              = Paths.get(resourceFolder,"resource.bin").toFile().getAbsolutePath();

        LOG.info("Service initialized");
    }

    @Override
    public Inference createInference(String text, boolean topics) throws AvroRemoteException {
        try {

            List<Double> shape = inferencePoolManager.get(Thread.currentThread()).inference(text);
            if (!topics) return Inference.newBuilder().setVector(shape).build();

            TopicSummaryBuilder tsBuilder = new TopicSummaryBuilder(shape);


//            List<TopicWord> topTopics = IntStream.range(0, shape.size()).mapToObj(i -> TopicWord.newBuilder().setScore(shape.get(i)).setValue("" + i).build()).filter(w -> w.getScore() > (1.0 / Double.valueOf(shape.size()))).sorted((a, b) -> -a.getScore().compareTo(b.getScore())).collect(Collectors.toList());

            List<TopicSummary> topicList = getTopics();
            List<TopicSummary> topTopics = tsBuilder.getTopTopics().stream().map(i -> topicList.get(i)).collect(Collectors.toList());

            return Inference.newBuilder().setVector(shape).setTopics(topTopics).build();
        } catch (Exception e) {
            throw new AvroRemoteException("Error loading topic model",e);
        }
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

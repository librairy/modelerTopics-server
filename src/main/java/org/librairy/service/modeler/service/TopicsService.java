package org.librairy.service.modeler.service;

import cc.mallet.topics.ModelLauncher;
import cc.mallet.topics.ModelParams;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.builders.TFIDFTopicBuilder;
import org.librairy.service.modeler.facade.model.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class TopicsService {

    private static final Logger LOG = LoggerFactory.getLogger(TopicsService.class);

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    private String resourceFolder;

    @Autowired
    ModelLauncher modelLauncher;

    private Settings settings;
    private Map<Integer,Topic> topics = new HashMap<>();
    private Map<Integer, List<TopicNeighbour>> topicNeighbours = new HashMap<>();
    private Map<Integer, List<TopicWord>> words = new HashMap<>();
    private Map<Integer, List<TopicWord>> tfidfWords = new HashMap<>();
    private ModelParams parameters;

    @PostConstruct
    public void setup() throws Exception {
        if (modelLauncher.existsModel(resourceFolder)) loadModel();
        else LOG.warn("No found model!");
    }

    @PreDestroy
    public void destroy(){

    }

    public void remove(){
        modelLauncher.removeModel(resourceFolder);
        topics          = new HashMap<>();
        settings        = new Settings();
        words           = new ConcurrentHashMap<>();
        topicNeighbours = new ConcurrentHashMap<>();
        tfidfWords      = new ConcurrentHashMap<>();
    }

    public void loadModel() throws Exception {
        LOG.info("Reading existing topic model");
        parameters          = modelLauncher.readParameters(resourceFolder);
        settings            = modelLauncher.getDetails(resourceFolder);
        topics              = modelLauncher.readTopics(resourceFolder);
        words               = modelLauncher.readTopicWords(resourceFolder);
        topicNeighbours     = modelLauncher.readTopicNeighbours(resourceFolder);
        tfidfWords          = TFIDFTopicBuilder.calculate(words);
        LOG.info("Model ready!");
    }

    public List<TopicNeighbour> getNeighbours(Integer topicId, Integer max, Similarity similarity){
        if (!topicNeighbours.containsKey(topicId)) return Collections.emptyList();
        return  topicNeighbours.get(topicId).stream().filter(tc -> tc.getSimilarity().equals(similarity))
                .limit(max)
                .map(tn -> TopicNeighbour.newBuilder().setId(tn.getId()).setDescription(topics.get(tn.getId()).getDescription()).setScore(tn.getScore()).setSimilarity(tn.getSimilarity()).build())
                .collect(Collectors.toList());
    }

    public Map<Integer,List<TopicWord>> getTopWords(ParallelTopicModel topicModel, int numWords) throws Exception {

        int numTopics = topicModel.getNumTopics();
        Alphabet alphabet = topicModel.getAlphabet();

        ArrayList<TreeSet<IDSorter>> topicSortedWords = topicModel.getSortedWords();

        Map<Integer,List<TopicWord>> result = new HashMap<>();

        for (int topic = 0; topic < numTopics; topic++) {

            TreeSet<IDSorter> sortedWords = topicSortedWords.get(topic);

            if (sortedWords.isEmpty()){
                result.put(topic,Collections.emptyList());
                continue;
            }

            Double totalWeight = sortedWords.stream().map(w -> w.getWeight()).reduce((w1, w2) -> w1 + w2).get();

            // How many words should we report? Some topics may have fewer than
            //  the default number of words with non-zero weight.
            int limit = numWords<0? sortedWords.size() : numWords;
            if (sortedWords.size() < numWords) { limit = sortedWords.size(); }

            LOG.info("Topic " + topic + " with " + limit + " words");

            List<TopicWord> words = new ArrayList<>();

            Iterator<IDSorter> iterator = sortedWords.iterator();
            for (int i=0; i < limit; i++) {
                IDSorter info = iterator.next();
                words.add(new TopicWord(String.valueOf(alphabet.lookupObject(info.getID())),info.getWeight()/totalWeight));
            }
            result.put(topic,words);
        }

        return result;
    }

    public Topic get(Integer id){
        if (!topics.containsKey(id)) throw new RuntimeException("Topic Id not-found");
        return topics.get(id);
    }

    public List<TopicSummary> getTopics() throws AvroRemoteException {
        return topics.entrySet().stream().sorted((a,b) -> a.getKey().compareTo(b.getKey())).map(entry -> TopicSummary.newBuilder().setId(entry.getKey()).setDescription(entry.getValue().getDescription()).build()).collect(Collectors.toList());
    }

    public Settings getSettings() throws AvroRemoteException {
        return settings;
    }


    public List<TopicWord> getWords(int topicId, int maxWords, int offset) throws AvroRemoteException {
        if (!words.containsKey(topicId)) return Collections.emptyList();
        return words.get(topicId).stream().skip(offset).limit(maxWords).collect(Collectors.toList());

    }

    public List<TopicWord> getWordsByTFIDF(int topicId, int maxWords, int offset) throws AvroRemoteException {
        if (!words.containsKey(topicId)) return Collections.emptyList();
        return tfidfWords.get(topicId).stream().skip(offset).limit(maxWords).collect(Collectors.toList());

    }

    private Double tfidf(TopicWord el){
        Double tf = el.getScore();
        Double idf = idf(el.getValue());
        return tf*idf;
    }

    private Double idf(String term){
        // total docs
        int n = words.size();
        // docs with term
        long d = words.entrySet().stream().filter(entry -> entry.getValue().stream().filter(el -> el.getValue().equalsIgnoreCase(term)).count() > 0).count();
        if (d == 0) return 0.0;
        return Math.log(Double.valueOf(n)/Double.valueOf(d));
    }

    public ModelParams getParameters() {
        return parameters;
    }
}

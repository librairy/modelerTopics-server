package org.librairy.service.modeler.service;

import cc.mallet.topics.ModelLauncher;
import cc.mallet.topics.ModelParams;
import cc.mallet.topics.ParallelTopicModel;
import cc.mallet.types.Alphabet;
import cc.mallet.types.IDSorter;
import cc.mallet.util.ParallelExecutor;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.Dimension;
import org.librairy.service.modeler.facade.model.Element;
import org.librairy.service.modeler.facade.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
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

    private Model model;
    private List topics = new ArrayList();
    private Map<Integer, List<Element>> words = new HashMap<>();
    private Map<Integer, List<Element>> tfidfWords = new HashMap<>();
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
        topics = new ArrayList();
        model = new Model();
        words = new ConcurrentHashMap<>();
        tfidfWords = new ConcurrentHashMap<>();
    }

    public void loadModel() throws Exception {
        LOG.info("Reading existing topic model");
        parameters  = modelLauncher.readParameters(resourceFolder);
        model       = modelLauncher.getDetails(resourceFolder);
        topics      = modelLauncher.readTopics(resourceFolder);
        words       = modelLauncher.readTopicWords(resourceFolder);

        // create TF/IDF visualization of word topics
        LOG.info("Calculating TF-IDF terms score for topic sorting..");
        ParallelExecutor executor = new ParallelExecutor();
        for(Integer topicId: words.keySet()){
            final Integer id = topicId;
            executor.submit(() -> tfidfWords.put(id, words.get(id).stream().map(el -> new Element(el.getValue(), tfidf(el))).sorted((a, b) -> -a.getScore().compareTo(b.getScore())).collect(Collectors.toList()) ));
        }
        executor.awaitTermination(1, TimeUnit.HOURS);

        LOG.info("Model ready!");
    }

    public Map<Integer,List<Element>> getTopWords(ParallelTopicModel topicModel, int numWords) throws Exception {

        int numTopics = topicModel.getNumTopics();
        Alphabet alphabet = topicModel.getAlphabet();

        ArrayList<TreeSet<IDSorter>> topicSortedWords = topicModel.getSortedWords();

        Map<Integer,List<Element>> result = new HashMap<>();

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

            List<Element> words = new ArrayList<>();

            Iterator<IDSorter> iterator = sortedWords.iterator();
            for (int i=0; i < limit; i++) {
                IDSorter info = iterator.next();
                words.add(new Element(String.valueOf(alphabet.lookupObject(info.getID())),info.getWeight()/totalWeight));
            }
            result.put(topic,words);
        }

        return result;
    }

    public List<Dimension> getTopics() throws AvroRemoteException {
        return topics;
    }

    public Model getModel() throws AvroRemoteException {
        return model;
    }


    public List<Element> getWords(int topicId, int maxWords, int offset) throws AvroRemoteException {
        if (!words.containsKey(topicId)) return Collections.emptyList();
        //return words.get(topicId).stream().sorted((a,b) -> -a.getScore().compareTo(b.getScore())).skip(offset).limit(maxWords).collect(Collectors.toList());
        return words.get(topicId).stream().skip(offset).limit(maxWords).collect(Collectors.toList());

    }

    public List<Element> getWordsByTFIDF(int topicId, int maxWords, int offset) throws AvroRemoteException {
        if (!words.containsKey(topicId)) return Collections.emptyList();
        //return words.get(topicId).stream().sorted((a,b) -> -a.getScore().compareTo(b.getScore())).skip(offset).limit(maxWords).collect(Collectors.toList());
        return tfidfWords.get(topicId).stream().skip(offset).limit(maxWords).collect(Collectors.toList());

    }

    private Double tfidf(Element el){
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

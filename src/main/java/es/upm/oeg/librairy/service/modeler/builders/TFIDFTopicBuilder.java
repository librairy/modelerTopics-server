package es.upm.oeg.librairy.service.modeler.builders;

import es.upm.oeg.librairy.service.modeler.facade.model.TopicWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 *
 * Exploring a corpus through a topic model typically begins with visualizing the posterior topics through their per-topicterm probabilities β.
 * The simplest way to visualize a topic is to order the terms by their probability.
 *
 * However this is another approach inspired by the popular TFIDF term score of vocabulary terms used in information retrieval Baeza-Yates and Ribeiro-Neto (1999).
 * The first expression is akin to the term frequency; the second expression is akin to the document frequency, down-weighting terms that have high probability under all the topics.
 * Other methods of determining the difference between a topic and others can be found in (Tang and MacLennan, 2005).
 *
 * Reference: http://www.cs.columbia.edu/~blei/papers/BleiLafferty2009.pdf
 *
 */
public class TFIDFTopicBuilder {

    private static final Logger LOG = LoggerFactory.getLogger(TFIDFTopicBuilder.class);


    public static Map<Integer, List<TopicWord>> calculate(Map<Integer, List<TopicWord>> topics){

        LOG.info("Calculating TF-IDF terms score for topic sorting..");

        Map topicTFIDFWords = new HashMap<Integer,List<TopicWord>>();

        int k = topics.size();

        Map<String, List<TopicWord>> scoreWords = topics.entrySet().parallelStream().flatMap(entry -> entry.getValue().stream()).collect(Collectors.groupingByConcurrent(el -> el.getValue()));

        for(Map.Entry<Integer, List<TopicWord>> topic : topics.entrySet()){
            List<TopicWord> topicWords = topic.getValue();

            List<TopicWord> tfidfWords = topicWords.parallelStream().map(el -> new TopicWord(el.getValue(), el.getScore() * Math.log(k / scoreWords.get(el.getValue()).size()))).sorted((a, b) -> -a.getScore().compareTo(b.getScore())).collect(Collectors.toList());

            topicTFIDFWords.put(topic.getKey(), tfidfWords);

        }


        return topicTFIDFWords;

    }


    public static void main(String[] args) {

        Map<Integer,List<TopicWord>> topics = new HashMap<>();

        topics.put(0, Arrays.asList(new TopicWord("a",0.4),new TopicWord("b",0.4),new TopicWord("c",0.2)));
        topics.put(1, Arrays.asList(new TopicWord("c",0.8),new TopicWord("d",0.6),new TopicWord("b",0.2), new TopicWord("a",0.1)));
        topics.put(2, Arrays.asList(new TopicWord("a",0.5),new TopicWord("b",0.4),new TopicWord("d",0.4)));


        LOG.info("Topic Words: ");
        for(Map.Entry<Integer, List<TopicWord>> topicWords : topics.entrySet()){
            LOG.info("Topic " + topicWords.getKey());
            for(TopicWord el: topicWords.getValue()){
                LOG.info("\t - " + el);
            }
        }

        Map<Integer, List<TopicWord>> tfidfTopics = calculate(topics);

        LOG.info("TF/IDF Topic Words: " + tfidfTopics);
        for(Map.Entry<Integer, List<TopicWord>> topicWords : tfidfTopics.entrySet()){
            LOG.info("Topic " + topicWords.getKey());
            for(TopicWord el: topicWords.getValue()){
                LOG.info("\t - " + el);
            }
        }

    }

}

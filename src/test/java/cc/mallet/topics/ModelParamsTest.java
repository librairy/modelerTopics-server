package cc.mallet.topics;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class ModelParamsTest {

    private static final Logger LOG = LoggerFactory.getLogger(ModelParamsTest.class);

    @Test
    public void serialize() throws JsonProcessingException {
        ModelParams params = new ModelParams();
        params.setStopwords(Collections.emptyList());
        params.setNumTopWords(50);
        params.setNumRetries(10);
        params.setAlpha(0.1);
        params.setBeta(0.001);
        params.setLanguage("en");
        params.setNumIterations(1000);
        params.setNumTopics(10);
        params.setPos("VERB NOUN ADVERB ADJECTIVE");


        ObjectMapper jsonMapper = new ObjectMapper();
        LOG.info("JSON: " + jsonMapper.writeValueAsString(params));


    }

}

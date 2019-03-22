package es.upm.oeg.librairy.service.modeler.client;

import org.junit.Test;
import es.upm.oeg.librairy.service.modeler.clients.LibrairyNlpClient;
import org.librairy.service.nlp.facade.model.Group;
import org.librairy.service.nlp.facade.model.PoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class LibrairyNlpClientIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(LibrairyNlpClientIntTest.class);

    private static final String text = "Intuitively, you can think of a CharMatcher as representing a particular class of characters, like digits or whitespace. Practically speaking, a CharMatcher is just a boolean predicate on characters -- indeed, CharMatcher implements [Predicate<Character>] -- but because it is so common to refer to \"all whitespace characters\" or \"all lowercase letters,\" Guava provides this specialized syntax and API for characters.";

    @Test
    public void bowHttp(){

        LibrairyNlpClient client = new LibrairyNlpClient();
        client.setNlpEndpoint("http://localhost:8085");
        client.setup();
        List<Group> tokens = client.bow(text, "en", Arrays.asList(new PoS[]{PoS.NOUN, PoS.VERB, PoS.ADVERB}), false);
        tokens.forEach(token -> LOG.info(""+token));
    }

    @Test
    public void bowAVRO(){

        LibrairyNlpClient client = new LibrairyNlpClient();
        client.setNlpEndpoint("localhost");
        client.setup();
        List<Group> tokens = client.bow(text, "en", Arrays.asList(new PoS[]{PoS.NOUN, PoS.VERB, PoS.ADVERB}), false);
        tokens.forEach(token -> LOG.info(""+token));
    }

}

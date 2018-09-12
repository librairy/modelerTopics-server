package org.librairy.service.modeler.builders;

import cc.mallet.pipe.*;
import com.google.common.base.Strings;
import org.librairy.service.nlp.facade.model.PoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class RawPipeBuilder implements PipeBuilderI {

    private static final Logger LOG = LoggerFactory.getLogger(RawPipeBuilder.class);

    public RawPipeBuilder() {
    }

    public Pipe build(String pos) {
        ArrayList pipeList = new ArrayList();

        // Read data from File objects
        pipeList.add(new Input2CharSequence("UTF-8"));

        // Regular expression for what constitutes a token.
        //  This pattern includes Unicode letters, Unicode numbers,
        //   and the underscore character. Alternatives:
        //    "\\S+"   (anything not whitespace)
        //    "\\w+"    ( A-Z, a-z, 0-9, _ )
        //    "[\\p{L}\\p{N}_]+|[\\p{P}]+"   (a group of only letters and numbers OR
        //                                    a group of only punctuation marks)
        //pipeList.add(new CharSequence2TokenSequence(Pattern.compile("[\\p{L}\\p{N}_]+")));
        pipeList.add(new CharSequence2TokenSequence(Pattern.compile("\\S+")));

        pipeList.add(new TokenSequenceRemoveStopwords(false, false));

        pipeList.add(new TokenSequence2FeatureSequence());


        // Now convert the sequence of features to a sparse vector,
        //  mapping feature IDs to counts.
//        pipeList.add(new FeatureSequence2FeatureVector());


        // Print out the features and the label
//        pipeList.add(new PrintInputAndTarget());

        return new SerialPipes(pipeList);
    }



}

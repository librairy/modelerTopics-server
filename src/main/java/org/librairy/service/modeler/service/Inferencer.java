package org.librairy.service.modeler.service;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.ModelLauncher;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import com.google.common.base.Strings;
import com.google.common.primitives.Doubles;
import org.librairy.service.modeler.builders.PipeBuilder;
import org.librairy.service.modeler.clients.LibrairyNlpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class Inferencer {
    private static final Logger LOG = LoggerFactory.getLogger(Inferencer.class);

    private final TopicInferencer topicInferer;
    private final LibrairyNlpClient client;
    private final String language;

    public Inferencer(ModelLauncher ldaLauncher, LibrairyNlpClient client, String language, String resourceFolder) throws Exception {

        this.topicInferer               = ldaLauncher.getTopicInferencer(resourceFolder);
        this.client                     = client;
        this.language                   = language;
    }

    public Inferencer(TopicInferencer inferencer, LibrairyNlpClient client, String language) {
        this.topicInferer               = inferencer.clone();
        this.client                     = client;
        this.language                   = language;
    }



    public List<Double> inference(String s) throws Exception {

        if (Strings.isNullOrEmpty(s)) return Collections.emptyList();

        String data = s;
        String name = "";
        String source = "";
        String target = "";
        Integer numIterations = 1000;

        Instance rawInstance = new Instance(data,target,name,source);
        Pipe pipe = new PipeBuilder().build(client, language);
        InstanceList instances = new InstanceList(pipe);
        instances.addThruPipe(rawInstance);

        int thinning = 1;
        int burnIn = 0;
        double[] topicDistribution = topicInferer.getSampledDistribution(instances.get(0), numIterations, thinning, burnIn);


        LOG.debug("Topic Distribution of: " + s.substring(0,10)+ ".. " + Arrays.toString(topicDistribution));
        return Doubles.asList(topicDistribution);

    }

    public TopicInferencer getTopicInferer() {
        return topicInferer;
    }
}
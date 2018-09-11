package org.librairy.service.modeler.service;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.ModelLauncher;
import cc.mallet.topics.ModelParams;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import com.google.common.base.Strings;
import com.google.common.primitives.Doubles;
import org.librairy.service.modeler.builders.PipeBuilder;
import org.librairy.service.modeler.clients.LibrairyNlpClient;
import org.librairy.service.nlp.facade.model.Group;
import org.librairy.service.nlp.facade.model.PoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class Inferencer {
    private static final Logger LOG = LoggerFactory.getLogger(Inferencer.class);

    private final TopicInferencer topicInferer;
    private final LibrairyNlpClient client;
    private final String language;
    private final String pos;
    private final Boolean multigrams;
    private final List<PoS> posList;

    public Inferencer(ModelLauncher ldaLauncher, LibrairyNlpClient client, ModelParams params, String resourceFolder) throws Exception {

        this.topicInferer               = ldaLauncher.getTopicInferencer(resourceFolder);
        this.client                     = client;
        this.language                   = params.getLanguage();
        this.pos                        = params.getPos();
        this.posList                    = Strings.isNullOrEmpty(pos) ? Collections.emptyList() : Arrays.asList(pos.split(" ")).stream().map(i -> PoS.valueOf(i.toUpperCase())).collect(Collectors.toList());
        this.multigrams                 = params.getEntities();
    }

    public Inferencer(TopicInferencer inferencer, LibrairyNlpClient client, ModelParams params) {
        this.topicInferer               = inferencer.clone();
        this.client                     = client;
        this.language                   = params.getLanguage();
        this.pos                        = params.getPos();
        this.posList                    = Strings.isNullOrEmpty(pos) ? Collections.emptyList() : Arrays.asList(pos.split(" ")).stream().map(i -> PoS.valueOf(i.toUpperCase())).collect(Collectors.toList());
        this.multigrams                 = params.getEntities();
    }



    public List<Double> inference(String text) throws Exception {

        if (Strings.isNullOrEmpty(text)) return Collections.emptyList();


        List<Group> bows = client.bow(text, this.language, this.posList, this.multigrams);

        String data = BoWService.toText(bows);
        String name = "";
        String source = "";
        String target = "";
        Integer numIterations = 1000;


        Instance rawInstance = new Instance(data,target,name,source);
        Pipe pipe = new PipeBuilder().build(this.pos);
        InstanceList instances = new InstanceList(pipe);
        instances.addThruPipe(rawInstance);

        int thinning = 1;
        int burnIn = 0;
        double[] topicDistribution = topicInferer.getSampledDistribution(instances.get(0), numIterations, thinning, burnIn);

        String description = text.length() < 10 ? text : text.substring(0,10);

        LOG.debug("Topic Distribution of: " + description + ".. " + Arrays.toString(topicDistribution));
        return Doubles.asList(topicDistribution);

    }

    public TopicInferencer getTopicInferer() {
        return topicInferer;
    }
}
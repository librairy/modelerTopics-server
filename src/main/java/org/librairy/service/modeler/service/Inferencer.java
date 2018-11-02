package org.librairy.service.modeler.service;

import cc.mallet.pipe.Pipe;
import cc.mallet.topics.ModelLauncher;
import cc.mallet.topics.ModelParams;
import cc.mallet.topics.TopicInferencer;
import cc.mallet.types.Alphabet;
import cc.mallet.types.FeatureSequence;
import cc.mallet.types.Instance;
import cc.mallet.types.InstanceList;
import com.google.common.base.Strings;
import com.google.common.primitives.Doubles;
import org.librairy.service.modeler.builders.BoWPipeBuilder;
import org.librairy.service.modeler.builders.PipeBuilderFactory;
import org.librairy.service.modeler.clients.LibrairyNlpClient;
import org.librairy.service.nlp.facade.model.Group;
import org.librairy.service.nlp.facade.model.PoS;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
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
    private final Boolean raw;
    private final Integer iterations;
    private final Pipe pipe;
    private final Alphabet alphabet;

    public Inferencer(ModelLauncher ldaLauncher, LibrairyNlpClient client, ModelParams params, String resourceFolder, Alphabet alphabet, Pipe pipe) throws Exception {

        this.topicInferer               = ldaLauncher.getTopicInferencer(resourceFolder);
        this.client                     = client;
        this.language                   = params.getLanguage();
        this.pos                        = params.getPos();
        this.posList                    = Strings.isNullOrEmpty(pos) ? Collections.emptyList() : Arrays.asList(pos.split(" ")).stream().map(i -> PoS.valueOf(i.toUpperCase())).collect(Collectors.toList());
        this.multigrams                 = params.getEntities();
        this.raw                        = params.getRaw();
        this.iterations                 = params.getNumIterations();
        this.pipe                       = pipe;
        this.alphabet                   = alphabet;

    }

    public Inferencer(TopicInferencer inferencer, LibrairyNlpClient client, ModelParams params, Alphabet alphabet, Pipe pipe) {
        this.topicInferer               = inferencer.clone();
        this.client                     = client;
        this.language                   = params.getLanguage();
        this.pos                        = params.getPos();
        this.posList                    = Strings.isNullOrEmpty(pos) ? Collections.emptyList() : Arrays.asList(pos.split(" ")).stream().map(i -> PoS.valueOf(i.toUpperCase())).collect(Collectors.toList());
        this.multigrams                 = params.getEntities();
        this.raw                        = params.getRaw();
        this.iterations                 = params.getNumIterations();
        this.pipe                       = pipe;
        this.alphabet                   = alphabet;
    }


    public List<Double> inference(String text) throws Exception {

        if (Strings.isNullOrEmpty(text)) return Collections.emptyList();


        List<Group> bows = client.bow(text, this.language, this.posList, this.multigrams);

        String data = BoWService.toText(bows);
        String name = "";
        String source = null;
        String target = "";

//        InstanceList previousInstanceList = InstanceList.load(new File("src/test/bin/model/instances.data"));
//        Pipe pipe = previousInstanceList.getPipe();

        Instance rawInstance = new Instance(data,target,name,source);

//        Pipe pipe = PipeBuilderFactory.newInstance(raw).build(this.pos);

        InstanceList instances = new InstanceList(pipe);
        instances.addThruPipe(rawInstance);

        // Use model alphabet to set features
        FeatureSequence fs = (FeatureSequence) rawInstance.getData();
        FeatureSequence featureData = new FeatureSequence(alphabet);
        for( int i=0; i< fs.getLength(); i++){
            String feature = (String) fs.get(i);
            featureData.add(feature);
        }

        int thinning = 1;//1
        int burnIn = 5;//5
        double[] topicDistribution = topicInferer.getSampledDistribution(new Instance(featureData, target, name, source), iterations, thinning, burnIn);

        String description = text.length() < 10 ? text : text.substring(0,10);

        LOG.debug("Topic Distribution of: " + description + ".. " + Arrays.toString(topicDistribution));
        return Doubles.asList(topicDistribution);

    }

    public TopicInferencer getTopicInferer() {
        return topicInferer;
    }

    public Pipe getPipe() {
        return pipe;
    }

    public Alphabet getAlphabet() {
        return alphabet;
    }
}
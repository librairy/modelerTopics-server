package org.librairy.service.modeler.service;

import cc.mallet.topics.ModelLauncher;
import org.librairy.service.modeler.clients.LibrairyNlpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class InferencePoolManager {

    private static final Logger LOG = LoggerFactory.getLogger(InferencePoolManager.class);

    @Value("#{environment['NLP_ENDPOINT']?:'${nlp.endpoint}'}")
    String nlpEndpoint;

    @Value("#{environment['RESOURCE_FOLDER']?:'${resource.folder}'}")
    String resourceFolder;

    @Value("#{environment['SERVER_THREADS']?:${server.tomcat.max-threads}}")
    Integer maxThreads;

    @Autowired
    TopicsService topicsService;

    @Autowired
    ModelLauncher ldaLauncher;

    @Autowired
    LibrairyNlpClient client;

    Map<Long,Inferencer> inferencePool;

    @PostConstruct
    public void setup() throws Exception {

        inferencePool   = new ConcurrentHashMap<>();

        if (topicsService.getParameters() != null){

            LOG.info("Deserializing topic inferencer ..");
            initializeInferencer(0l);

            LOG.info("Initializing "+maxThreads+" parallel inferencer ..");
            ExecutorService executors = Executors.newWorkStealingPool();

            for(int i=1;i<maxThreads;i++){
                final Long id = Long.valueOf(i);
                executors.submit(() -> {
                    try {
                        initializeInferencer(id);
                    } catch (Exception e) {
                        LOG.warn("error initializing inferencer",e);
                    }
                });

            }
            executors.shutdown();
            executors.awaitTermination(1, TimeUnit.HOURS);
            LOG.info("done!");
        }


    }

    public Inferencer get(Thread thread) throws Exception {
        Long key = getKey(thread.getId());
        initializeInferencer(key);
        return inferencePool.get(key);

    }

    private void initializeInferencer(Long id) throws Exception {
        if (!inferencePool.containsKey(id) && topicsService.getParameters() != null){
            String language = topicsService.getParameters().getLanguage();
            Inferencer inferencer = inferencePool.containsKey(0l)? new Inferencer(inferencePool.get(0l).getTopicInferer(),client,language) : new Inferencer(ldaLauncher,client,language,resourceFolder);
            inferencePool.put(id,inferencer);
        }
    }


    private Long getKey(Long id){
        return id % maxThreads;
    }

    public void update(){
        this.inferencePool.clear();
    }
}

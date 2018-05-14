package org.librairy.service.modeler.service;

import cc.mallet.topics.ModelLauncher;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
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


    LoadingCache<Long, Inferencer> inferenceCache;

    Inferencer inferencer;


    @PostConstruct
    public void setup() throws Exception {

        if (topicsService.getParameters() != null){
            LOG.info("Deserializing topic inferencer ..");
            String language = topicsService.getParameters().getLanguage();
            this.inferencer = new Inferencer(ldaLauncher,client,language,resourceFolder);
            LOG.info("done!");
        }

        inferenceCache = CacheBuilder.newBuilder()
                .maximumSize(maxThreads)
                .build(
                        new CacheLoader<Long, Inferencer>() {
                            public Inferencer load(Long key) {
                                LOG.info("Inferencer cloned for thread: " + key + " /  Total:" + (inferenceCache.size()+1));
                                String language = topicsService.getParameters().getLanguage();
                                return new Inferencer(inferencer.getTopicInferer(), client, language);
                            }
                });

    }

    public Inferencer get(Thread thread) throws Exception {
        Long key = getKey(thread.getId());
        return this.inferenceCache.get(key);
    }


    private Long getKey(Long id){
        return id % maxThreads;
    }

}

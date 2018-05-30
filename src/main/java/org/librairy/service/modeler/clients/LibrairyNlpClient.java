package org.librairy.service.modeler.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.cache.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.avro.AvroRemoteException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.librairy.service.nlp.facade.AvroClient;
import org.librairy.service.nlp.facade.model.Form;
import org.librairy.service.nlp.facade.model.PoS;
import org.librairy.service.nlp.facade.model.Token;
import org.librairy.service.nlp.facade.rest.model.ProcessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@Component
public class LibrairyNlpClient {

    private static final Logger LOG = LoggerFactory.getLogger(LibrairyNlpClient.class);


    static{
        Unirest.setDefaultHeader("Accept", MediaType.APPLICATION_JSON_VALUE);
        Unirest.setDefaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);

        com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper = new com.fasterxml.jackson.databind.ObjectMapper();
//        jacksonObjectMapper.enable(SerializationFeature.INDENT_OUTPUT);
        jacksonObjectMapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

        Unirest.setObjectMapper(new ObjectMapper() {

            public <T> T readValue(String value, Class<T> valueType) {
                try {
                    return jacksonObjectMapper.readValue(value, valueType);
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

            public String writeValue(Object value) {
                try {
                    return jacksonObjectMapper.writeValueAsString(value);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
            }
        });
    }


    @Value("#{environment['NLP_ENDPOINT']?:'${nlp.endpoint}'}")
    String nlpEndpoint;

    LoadingCache<String, AvroClient> clients;


    public void setNlpEndpoint(String nlpEndpoint) {
        this.nlpEndpoint = nlpEndpoint;
    }

    @PostConstruct
    public void setup(){
        clients = CacheBuilder.newBuilder()
                .maximumSize(100)
                .removalListener(new RemovalListener<String, AvroClient>() {
                    @Override
                    public void onRemoval(RemovalNotification<String, AvroClient> removalNotification) {
                        removalNotification.getValue().close();
                        LOG.info("AVRO connection closed: " + removalNotification.getKey());
                    }
                })
                .build(
                        new CacheLoader<String, AvroClient>() {
                            public AvroClient load(String key) throws IOException {
                                AvroClient client = new AvroClient();
                                String language = StringUtils.substringAfter(key,"-lang:");
                                String nlpServiceEndpoint = nlpEndpoint.replace("%%", language);
                                try {
                                    LOG.info("Creating a new AVRO connection to: " + nlpServiceEndpoint);
                                    client.open(nlpServiceEndpoint,65111);
                                    return client;
                                } catch (Exception e) {
                                    LOG.error("Error connecting to nlp service: " + nlpServiceEndpoint, e);
                                    throw e;
                                }
                            }
                        });
    }

    @PreDestroy
    public void shutdown(){
        clients.cleanUp();
    }


    public String lemmatize(String text, String language, List<PoS> poSList){
        String key = "Thread-"+ Thread.currentThread().getId()+"-lang:"+language;
        if (nlpEndpoint.startsWith("http")) return lemmatizeByHttp(text,language,poSList, key);
        else return lemmatizeByAVRO(text,language,poSList, key);
    }


    private String lemmatizeByAVRO(String text, String language, List<PoS> poSList, String key){
        try {
            AvroClient client = clients.get(key);
            return client.process(text,poSList, Form.LEMMA);
        } catch (Exception e) {
            LOG.error("Error retrieving lemmas from nlp service: " + nlpEndpoint.replace("%%", language), e);
        }
        return "";
    }

    private String lemmatizeByHttp(String text, String language, List<PoS> poSList, String key){
        String nlpServiceEndpoint = nlpEndpoint.replace("%%", language);

        try {
            ProcessRequest request = new ProcessRequest();
            request.setFilter(poSList);
            request.setForm(Form.LEMMA);
            request.setText(text);
            HttpResponse<JsonNode> response = Unirest.post(nlpServiceEndpoint + "/process").
                    body(request).
                    asJson();
            if (response.getStatus() == 200) {
                return response.getBody().getObject().getString("processedText");
            }else{
                LOG.warn("Response error from nlp-service: " + response.getStatus() + ": " + response.getStatusText());
            }
        } catch (UnirestException e) {
            LOG.warn("Error getting lemma from NLP service",e);
        }


        return "";
    }

    public List<Token> bow(String text, String language, List<PoS> poSList){
        String key = "Thread-"+ Thread.currentThread().getId()+"-lang:"+language;
        if (nlpEndpoint.startsWith("http")) return bowByHttp(text,language,poSList, key);
        else return bowByAVRO(text,language,poSList, key);
    }


    private List<Token> bowByAVRO(String text, String language, List<PoS> poSList, String key){
        try {
            AvroClient client = clients.get(key);
            return client.group(text,poSList, Form.LEMMA);
        } catch (Exception e) {
            LOG.error("Error retrieving bow from nlp service: " + nlpEndpoint.replace("%%", language), e);
        }
        return Collections.emptyList();
    }

    private List<Token> bowByHttp(String text, String language, List<PoS> poSList, String key){
        String nlpServiceEndpoint = nlpEndpoint.replace("%%", language);

        List<Token> tokens = new ArrayList<>();
        try {
            ProcessRequest request = new ProcessRequest();
            request.setFilter(poSList);
            request.setText(text);
            HttpResponse<JsonNode> response = Unirest.post(nlpServiceEndpoint + "/group").
                    body(request).
                    asJson();
            if (response.getStatus() == 200){
                JSONArray tokenList = response.getBody().getObject().getJSONArray("tokens");
                for(int i=0;i<tokenList.length();i++){
                    JSONObject json = tokenList.getJSONObject(i);
                    Token token = Token.newBuilder().setTarget(json.getString("target")).setLemma(json.getString("lemma")).setFreq(json.getInt("freq")).setPos(PoS.valueOf(json.getString("pos").toUpperCase())).build();
                    tokens.add(token);
                }

            }else{
                LOG.warn("Response error from nlp-service: " + response.getStatus() + ": " + response.getStatusText());
            }
        } catch (UnirestException e) {
            LOG.warn("Error getting lemma from NLP service",e);
        }


        return tokens;
    }

}

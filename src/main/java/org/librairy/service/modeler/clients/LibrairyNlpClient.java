package org.librairy.service.modeler.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.nlp.facade.AvroClient;
import org.librairy.service.nlp.facade.model.Form;
import org.librairy.service.nlp.facade.model.PoS;
import org.librairy.service.nlp.facade.rest.model.ProcessRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;
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


    Map<String,AvroClient> clients;

    @PostConstruct
    public void setup(){
        clients = new ConcurrentHashMap<>();
    }

    @PreDestroy
    public void shutdown(){
        if (!clients.isEmpty()){
            clients.entrySet().stream().forEach(entry -> entry.getValue().close());
            clients.clear();
        }
    }


    public String lemmatize(String text, String language, List<PoS> poSList){
        String key = "Thread-"+ Thread.currentThread().getId();
        if (nlpEndpoint.startsWith("http")) return lemmatizeByHttp(text,language,poSList, key);
        else return lemmatizeByAVRO(text,language,poSList, key);
    }


    private String lemmatizeByAVRO(String text, String language, List<PoS> poSList, String key){
        if (!clients.containsKey(key)){
            AvroClient client = new AvroClient();
            String nlpServiceEndpoint = nlpEndpoint.replace("%%", language);
            try {
                LOG.info("Creating a new AVRO connection to: " + nlpServiceEndpoint);
                client.open(nlpServiceEndpoint,65111);
                clients.put(key,client);
            } catch (IOException e) {
                LOG.error("Error connecting to nlp service: " + nlpServiceEndpoint, e);
            }
        }
        AvroClient client = clients.get(key);
        try {
            return client.process(text,poSList, Form.LEMMA);
        } catch (AvroRemoteException e) {
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
            if (response.getStatus() == 200) return response.getBody().getObject().getString("processedText");
        } catch (UnirestException e) {
            LOG.warn("Error getting lemma from NLP service",e);
        }


        return "";
    }

}

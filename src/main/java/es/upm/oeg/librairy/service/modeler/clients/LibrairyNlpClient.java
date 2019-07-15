package es.upm.oeg.librairy.service.modeler.clients;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.google.common.cache.*;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.json.JSONArray;
import org.json.JSONObject;
import org.librairy.service.nlp.facade.AvroClient;
import org.librairy.service.nlp.facade.model.Form;
import org.librairy.service.nlp.facade.model.Group;
import org.librairy.service.nlp.facade.model.PoS;
import org.librairy.service.nlp.facade.rest.model.GroupsRequest;
import org.librairy.service.nlp.facade.rest.model.TokensRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.thymeleaf.util.StringUtils;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import java.io.IOException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

        try {

            TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }

            } };


            SSLContext sslcontext = SSLContext.getInstance("SSL");
            sslcontext.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sslcontext.getSocketFactory());
            SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(sslcontext);
            CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslsf).build();
            Unirest.setHttpClient(httpclient);

        } catch (Exception e) {
            LOG.error("HTTP Error",e);
        }


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
                                Integer port = 65111;
                                String url = nlpEndpoint;
                                if (nlpEndpoint.contains(":")){
                                    port = Integer.valueOf(org.apache.commons.lang3.StringUtils.substringAfterLast(nlpEndpoint,":"));
                                    url = org.apache.commons.lang3.StringUtils.substringBefore(nlpEndpoint,":");
                                }
                                String nlpServiceEndpoint = url.replace("%%", language);
                                try {
                                    LOG.info("Creating a new AVRO connection to: " + nlpServiceEndpoint);
                                    client.open(nlpServiceEndpoint,port);
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


    public String lemmatize(String text, String language, List<PoS> poSList, Boolean entities){
        String key = "Thread-"+ Thread.currentThread().getId()+"-lang:"+language;
        if (nlpEndpoint.startsWith("http")) return lemmatizeByHttp(text,language,poSList, key, entities);
        else return lemmatizeByAVRO(text,language,poSList, key, entities);
    }


    private String lemmatizeByAVRO(String text, String language, List<PoS> poSList, String key, Boolean entities){
        try {
            AvroClient client = clients.get(key);
            return client.tokens(text,poSList, Form.LEMMA, entities, language);
        } catch (Exception e) {
            LOG.error("Error retrieving lemmas from nlp service: " + nlpEndpoint.replace("%%", language), e);
        }
        return "";
    }

    private String lemmatizeByHttp(String text, String language, List<PoS> poSList, String key, Boolean multigrams){

        try {
            TokensRequest request = new TokensRequest();
            request.setFilter(poSList);
            request.setForm(Form.LEMMA);
            request.setLang(language);
            request.setText(text);
            request.setMultigrams(multigrams);
            HttpResponse<JsonNode> response = Unirest.post(nlpEndpoint + "/tokens").
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

    public List<Group> bow(String text, String language, List<PoS> poSList, Boolean entities){
        String key = "Thread-"+ Thread.currentThread().getId()+"-lang:"+language;
        if (nlpEndpoint.startsWith("http")) return bowByHttp(text,language,poSList, key, entities);
        else return bowByAVRO(text,language,poSList, key, entities);
    }


    private List<Group> bowByAVRO(String text, String language, List<PoS> poSList, String key, Boolean multigrams){
        try {
            AvroClient client = clients.get(key);
            return client.groups(text,poSList, Form.LEMMA, multigrams, false, false, language);
        } catch (Exception e) {
            LOG.error("Error retrieving bow from nlp service: " + nlpEndpoint.replace("%%", language), e);
        }
        return Collections.emptyList();
    }

    private List<Group> bowByHttp(String text, String language, List<PoS> poSList, String key, Boolean multigrams){

        List<Group> tokens = new ArrayList<>();
        try {
            GroupsRequest request = new GroupsRequest();
            request.setFilter(poSList);
            request.setText(text);
            request.setMultigrams(multigrams);
            request.setLang(language);
            HttpResponse<JsonNode> response = Unirest.post(nlpEndpoint + "/groups").
                    body(request).
                    asJson();
            if (response.getStatus() == 200){
                JSONArray tokenList = response.getBody().getObject().getJSONArray("groups");
                for(int i=0;i<tokenList.length();i++){
                    JSONObject json = tokenList.getJSONObject(i);
                    Group token = Group.newBuilder().setToken(json.getString("token")).setFreq(json.getInt("freq")).setPos(PoS.valueOf(json.getString("pos").toUpperCase())).build();
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

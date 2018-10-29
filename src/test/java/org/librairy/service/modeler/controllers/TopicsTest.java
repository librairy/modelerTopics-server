package org.librairy.service.modeler.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.ImmutableMap;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

public class TopicsTest {

    private static final Logger LOG = LoggerFactory.getLogger(TopicsTest.class);

    @Before
    public void setup(){
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

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

    @Test
    @Ignore
    public void vocabulary() throws UnirestException {

        String baseUrl = "http://librairy.linkeddata.es/patstat-model";


        // Get Topic List

        HttpResponse<JsonNode> response = Unirest.get(baseUrl + "/topics").asJson();

        JSONArray topicList = response.getBody().getObject().getJSONArray("topics");

        Set<String> invalidWords = new TreeSet<>();

        for(int i=0; i<topicList.length();i++){

            JSONObject topic = topicList.getJSONObject(i);

            int topicId = topic.getInt("id");

            LOG.info("Analyzing topic '" + topicId + "' ");

            Integer offset = 0;

            Integer maxWords = 100;

            Boolean finished = false;

            Integer numWords = 0;

            while(!finished){
                HttpResponse<JsonNode> responseByTopic = Unirest.get(baseUrl + "/topics/"+topicId)
                        .queryString(ImmutableMap.of("maxWords",maxWords,"offset",offset))
                        .asJson();

                JSONArray elements = responseByTopic.getBody().getObject().getJSONArray("elements");

                for (int j=0;j<elements.length();j++){

                    String word = elements.getJSONObject(j).getString("value");
                    if (word.startsWith("<")) invalidWords.add(word);

                }


                offset += maxWords;

                numWords += elements.length();

                finished = elements.length() != maxWords;
            }

            LOG.info("Topic '" + topicId + "' described by " + numWords + " words");

        }
        LOG.info("Invalid Words: " + invalidWords.stream().collect(Collectors.joining(" ")));

    }
}
package es.upm.oeg.librairy.service.modeler.controllers;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.ObjectMapper;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import es.upm.oeg.librairy.service.modeler.Application;
import es.upm.oeg.librairy.service.modeler.facade.rest.model.InferenceRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class RestTest {

    private static final Logger LOG = LoggerFactory.getLogger(RestTest.class);

    @Before
    public void setup(){
        Unirest.setObjectMapper(new ObjectMapper() {
            private com.fasterxml.jackson.databind.ObjectMapper jacksonObjectMapper
                    = new com.fasterxml.jackson.databind.ObjectMapper();

            public <T> T readValue(String value, java.lang.Class<T> valueType) {
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
    public void post() throws UnirestException {

        InferenceRequest req = new InferenceRequest("sample text",false);

        HttpResponse<JsonNode> response = Unirest.post("http://localhost:7777/inference")
                .header("accept", "application/json")
                .header("Content-Type", "application/json")
                .body(req)
                .asJson();

        LOG.info("Response: " + response.getBody());


    }
}
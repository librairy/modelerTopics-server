package org.librairy.service.modeler.controllers;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.librairy.service.modeler.Application;
import org.librairy.service.modeler.facade.AvroClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Application.class})
@WebAppConfiguration
public class AvroIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(AvroIntTest.class);

    @Test
    public void inferenceTest() throws InterruptedException, IOException {

        AvroClient client = new AvroClient();


        String host     = "localhost";
        Integer port    = 65111;

        client.open(host,port);

        client.inference("sample text");

        client.close();
    }

}
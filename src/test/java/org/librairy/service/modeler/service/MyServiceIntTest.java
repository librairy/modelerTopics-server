package org.librairy.service.modeler.service;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.librairy.service.modeler.Application;
import org.librairy.service.modeler.facade.model.Inference;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = Application.class)
@WebAppConfiguration
public class MyServiceIntTest {

    private static final Logger LOG = LoggerFactory.getLogger(MyServiceIntTest.class);

    @Autowired
    ModelerService service;

    @Test
    public void inferenceTest() throws IOException {


        Inference result = service.createInference("sample text",false);

        LOG.info("Result: " + result);

//        Assert.assertEquals(2, annotations.size());
    }
}
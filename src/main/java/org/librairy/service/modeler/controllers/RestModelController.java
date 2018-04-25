package org.librairy.service.modeler.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.librairy.service.modeler.facade.rest.model.Inference;
import org.librairy.service.modeler.facade.rest.model.InferenceRequest;
import org.librairy.service.modeler.facade.rest.model.Model;
import org.librairy.service.modeler.facade.rest.model.Relevance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/model")
@Api(tags="/model", description="model details")
public class RestModelController {

    private static final Logger LOG = LoggerFactory.getLogger(RestModelController.class);

    @Autowired
    ModelerService service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }

    @ApiOperation(value = "params and stats about the model", nickname = "getModel", response=Model.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Model.class),
    })
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public Model model()  {
        try {
            org.librairy.service.modeler.facade.model.Model model = service.model();
            return (model == null)?  new Model() : new Model(model);
        } catch (AvroRemoteException e) {
            throw new RuntimeException(e);
        }
    }

}

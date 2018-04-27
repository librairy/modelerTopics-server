package org.librairy.service.modeler.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.librairy.service.modeler.facade.rest.model.Model;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

@RestController
@RequestMapping("/settings")
@Api(tags="/settings", description="model details")
public class RestSettingsController {

    private static final Logger LOG = LoggerFactory.getLogger(RestSettingsController.class);

    @Autowired
    ModelerService service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }

    @ApiOperation(value = "params and stats about the model", nickname = "getSettings", response=Model.class)
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

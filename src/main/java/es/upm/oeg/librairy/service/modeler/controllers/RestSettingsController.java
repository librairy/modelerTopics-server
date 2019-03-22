package es.upm.oeg.librairy.service.modeler.controllers;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.avro.AvroRemoteException;
import es.upm.oeg.librairy.service.modeler.facade.model.ModelerService;
import es.upm.oeg.librairy.service.modeler.facade.rest.model.Settings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

    @ApiOperation(value = "params and stats about the model", nickname = "getSettings", response=Settings.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Settings.class),
    })
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Settings> getSettings()  {
        try {
            es.upm.oeg.librairy.service.modeler.facade.model.Settings settings = service.getSettings();
            return (settings == null)?  new ResponseEntity<Settings>(new Settings(), HttpStatus.NO_CONTENT) : new ResponseEntity<Settings>(new Settings(settings), HttpStatus.OK) ;
        } catch (AvroRemoteException e) {
            throw new RuntimeException(e);
        }
    }

}

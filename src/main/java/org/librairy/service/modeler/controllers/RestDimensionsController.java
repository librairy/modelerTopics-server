package org.librairy.service.modeler.controllers;

import io.swagger.annotations.*;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.librairy.service.modeler.facade.rest.model.DimensionList;
import org.librairy.service.modeler.facade.rest.model.Element;
import org.librairy.service.modeler.facade.rest.model.ElementList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.stream.Collectors;

@RestController
@ConditionalOnProperty(value="modeler.dimensions.enabled")
@RequestMapping("/dimensions")
@Api(tags="/dimensions", description="topic details")
public class RestDimensionsController {

    private static final Logger LOG = LoggerFactory.getLogger(RestDimensionsController.class);

    @Autowired
    ModelerService service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }

    @ApiOperation(value = "list of topics", nickname = "postTopics", response=DimensionList.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = DimensionList.class),
    })
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public DimensionList dimensions()  {
        try {
            return new DimensionList(service.dimensions().stream().map(t -> new org.librairy.service.modeler.facade.rest.model.Dimension(t)).collect(Collectors.toList()));
        } catch (AvroRemoteException e) {
            throw new RuntimeException(e);
        }
    }


    @ApiOperation(value = "top words of a given topic", nickname = "getWords", response=ElementList.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = ElementList.class),
    })
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<ElementList> get(
            @ApiParam(value = "id", required = true) @PathVariable Integer id,
            @RequestParam(defaultValue = "25") Integer maxWords,
            @RequestParam(defaultValue = "0") Integer offset
            )  {
        try {
            return new ResponseEntity(new ElementList(service.elements(id,maxWords,offset).stream().map(w -> new Element(w)).collect(Collectors.toList())), HttpStatus.OK);
        } catch (AvroRemoteException e) {
            return new ResponseEntity("internal service seems down",HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e) {
            LOG.error("IO Error", e);
            return new ResponseEntity("IO error",HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

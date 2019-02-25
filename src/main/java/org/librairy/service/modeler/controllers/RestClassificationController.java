package org.librairy.service.modeler.controllers;

import com.google.common.base.Strings;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.librairy.service.modeler.facade.rest.model.ClassRequest;
import org.librairy.service.modeler.facade.rest.model.TopicSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/classes")
@Api(tags="/classes", description="topic classification")
public class RestClassificationController {

    private static final Logger LOG = LoggerFactory.getLogger(RestClassificationController.class);

    @Autowired
    ModelerService service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }

    @ApiOperation(value = "topics details in a text", nickname = "postClasses", response=TopicSummary.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response=TopicSummary.class, responseContainer = "List"),
    })
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<List<TopicSummary>> assignClasses(@RequestBody ClassRequest request)  {
        try {
            if (Strings.isNullOrEmpty(request.getText())){
                LOG.warn("Request is empty");
                MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
                headers.set("error-msg","request text is empty");
                return new ResponseEntity<List<TopicSummary>>(headers,HttpStatus.BAD_REQUEST);
            }
            List<TopicSummary> result = service.assignClasses(request.getText()).stream().map(ts -> new TopicSummary(ts)).collect(Collectors.toList());
            return new ResponseEntity<List<TopicSummary>>(result,HttpStatus.OK);
        } catch (AvroRemoteException e) {
            LOG.error("AVRO server error",e);
            return new ResponseEntity<List<TopicSummary>>(HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e){
            LOG.error("Unexpected error",e);
            return new ResponseEntity<List<TopicSummary>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

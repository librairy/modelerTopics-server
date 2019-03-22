package es.upm.oeg.librairy.service.modeler.controllers;

import com.google.common.base.Strings;
import es.upm.oeg.librairy.service.modeler.facade.model.ModelerService;
import es.upm.oeg.librairy.service.modeler.facade.rest.model.Inference;
import es.upm.oeg.librairy.service.modeler.facade.rest.model.InferenceRequest;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import org.apache.avro.AvroRemoteException;
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

@RestController
@RequestMapping("/inferences")
@Api(tags="/inferences", description="topic distributions")
public class RestInferencesController {

    private static final Logger LOG = LoggerFactory.getLogger(RestInferencesController.class);

    @Autowired
    ModelerService service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }

    @ApiOperation(value = "topics details in a text", nickname = "postInferences", response=Inference.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Inference.class),
    })
    @RequestMapping(method = RequestMethod.POST, produces = "application/json")
    public ResponseEntity<Inference> createInference(@RequestBody InferenceRequest request)  {
        try {
            if (Strings.isNullOrEmpty(request.getText())){
                LOG.warn("Request is empty");
                MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
                headers.set("error-msg","request text is empty");
                return new ResponseEntity<Inference>(headers,HttpStatus.BAD_REQUEST);
            }
            return new ResponseEntity(new Inference(service.createInference(request.getText(), request.getTopics())), HttpStatus.OK);
        } catch (AvroRemoteException e) {
            LOG.error("AVRO server error",e);
            return new ResponseEntity<Inference>(HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e){
            LOG.error("Unexpected error",e);
            return new ResponseEntity<Inference>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

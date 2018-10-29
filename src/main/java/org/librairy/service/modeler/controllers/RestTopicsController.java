package org.librairy.service.modeler.controllers;

import io.swagger.annotations.*;
import org.apache.avro.AvroRemoteException;
import org.librairy.service.modeler.facade.model.ModelerService;
import org.librairy.service.modeler.facade.model.Similarity;
import org.librairy.service.modeler.facade.rest.model.Topic;
import org.librairy.service.modeler.facade.rest.model.TopicNeighbour;
import org.librairy.service.modeler.facade.rest.model.TopicSummary;
import org.librairy.service.modeler.facade.rest.model.TopicWord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.util.List;
import java.util.stream.Collectors;

@RestController
@ConditionalOnProperty(value="modeler.dimensions.enabled")
@RequestMapping("/topics")
@Api(tags="/topics", description="topic details")
public class RestTopicsController {

    private static final Logger LOG = LoggerFactory.getLogger(RestTopicsController.class);

    @Autowired
    ModelerService service;

    @PostConstruct
    public void setup(){

    }

    @PreDestroy
    public void destroy(){

    }

    @ApiOperation(value = "list of topics", nickname = "getTopics", response=TopicSummary.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TopicSummary.class, responseContainer = "List"),
    })
    @RequestMapping(method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<TopicSummary>> getTopics()  {
        try {
            return new ResponseEntity<List<TopicSummary>>(service.getTopics().stream().map(ts -> new TopicSummary(ts)).collect(Collectors.toList()),HttpStatus.OK);
        } catch (AvroRemoteException e) {
            LOG.error("Internal Error", e);
            return new ResponseEntity<List<TopicSummary>>(HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e) {
            LOG.error("IO Error", e);
            return new ResponseEntity<List<TopicSummary>>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @ApiOperation(value = "topic info", nickname = "getTopic", response=Topic.class)
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = Topic.class),
    })
    @RequestMapping(value = "/{id:.+}", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<Topic> getTopic(
            @ApiParam(value = "id", required = true) @PathVariable Integer id
    )  {
        try {
            return new ResponseEntity<Topic>(new Topic(service.getTopic(id)),HttpStatus.OK);
        } catch (RuntimeException e) {
            LOG.error("Runtime Error: " + e.getMessage());
            return new ResponseEntity<Topic>(HttpStatus.NOT_FOUND);
        } catch (AvroRemoteException e) {
            LOG.error("Internal Error", e);
            return new ResponseEntity<Topic>(HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e) {
            LOG.error("IO Error", e);
            return new ResponseEntity<Topic>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @ApiOperation(value = "topic words", nickname = "getTopicWords", response=TopicWord.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TopicWord.class, responseContainer = "List"),
    })
    @RequestMapping(value = "/{id:.+}/words", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<TopicWord>> getTopicWords(
            @ApiParam(value = "id", required = true) @PathVariable Integer id,
            @RequestParam(defaultValue = "25") Integer max,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "false") Boolean tfidf
            )  {
        try {
            return new ResponseEntity<List<TopicWord>>(service.getTopicWords(id, max, offset, tfidf).stream().map(tw -> new TopicWord(tw)).collect(Collectors.toList()), HttpStatus.OK);
        } catch (AvroRemoteException e) {
            LOG.error("Internal Error", e);
            return new ResponseEntity(HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e) {
            LOG.error("IO Error", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }


    @ApiOperation(value = "topic neighbours", nickname = "getTopicNeighbours", response=TopicNeighbour.class, responseContainer = "List")
    @ApiResponses(value = {
            @ApiResponse(code = 200, message = "Success", response = TopicNeighbour.class, responseContainer = "List"),
    })
    @RequestMapping(value = "/{id:.+}/neighbours", method = RequestMethod.GET, produces = "application/json")
    public ResponseEntity<List<TopicNeighbour>> getTopicNeighbours(
            @ApiParam(value = "id", required = true) @PathVariable Integer id,
            @RequestParam(defaultValue = "25") Integer max
//            @RequestParam(defaultValue = "CONCURRENCE") Similarity similarity
    )  {
        try {
            return new ResponseEntity<List<TopicNeighbour>>(service.getTopicNeighbours(id, max, Similarity.CONCURRENCE).stream().map(tw -> new TopicNeighbour(tw)).collect(Collectors.toList()), HttpStatus.OK);
        } catch (AvroRemoteException e) {
            LOG.error("Internal Error", e);
            return new ResponseEntity(HttpStatus.FAILED_DEPENDENCY);
        } catch (Exception e) {
            LOG.error("IO Error", e);
            return new ResponseEntity(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

}

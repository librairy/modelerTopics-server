package org.librairy.service.modeler.service;

import com.google.common.escape.Escaper;
import com.google.common.escape.Escapers;
import org.librairy.service.nlp.facade.model.Group;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class BoWService {

    private static final Logger LOG = LoggerFactory.getLogger(BoWService.class);


    private final static Escaper escaper = Escapers.builder()
            .addEscape('\'',"")
            .addEscape('\"',"")
            .addEscape('\n'," ")
            .addEscape('\r'," ")
            .addEscape('\t'," ")
            .addEscape('=',"")
            .addEscape('#',"")
            .build();

    public static String toText(List<Group> groups){

        if (groups.isEmpty()) return "";

        return groups.stream().map(token -> escaper.escape(token.getToken()) + "=" + token.getFreq() + "#" + token.getPos() + "#").collect(Collectors.joining(" "));
    }

}

package es.upm.oeg.librairy.service.modeler.builders;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class PipeBuilderFactory {

    private static final Logger LOG = LoggerFactory.getLogger(PipeBuilderFactory.class);

    public static PipeBuilderI newInstance(Boolean raw){
        if (raw) return new RawPipeBuilder();
        else return new BoWPipeBuilder();
    }

}

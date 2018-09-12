package org.librairy.service.modeler.builders;

import cc.mallet.pipe.Pipe;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public interface PipeBuilderI {

    Pipe build(String pos);

}

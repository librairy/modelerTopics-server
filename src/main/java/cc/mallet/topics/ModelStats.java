package cc.mallet.topics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class ModelStats {

    private static final Logger LOG = LoggerFactory.getLogger(ModelStats.class);

    double loglikelihood;

    public ModelStats() {
    }

    public double getLoglikelihood() {
        return loglikelihood;
    }

    public void setLoglikelihood(double loglikelihood) {
        this.loglikelihood = loglikelihood;
    }
}

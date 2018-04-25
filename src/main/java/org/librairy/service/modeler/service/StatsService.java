package org.librairy.service.modeler.service;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */

public class StatsService {

    private static final Logger LOG = LoggerFactory.getLogger(StatsService.class);

    public static String from(int[] values){
        return from(Doubles.toArray(Ints.asList(values)));
    }

    public static String from(double[] valuesArray){
        StringBuilder stats = new StringBuilder();
        StandardDeviation stdDev = new StandardDeviation();
        stats.append("min=").append(Double.valueOf(StatUtils.min(valuesArray))).append("|");
        stats.append("max=").append(Double.valueOf(StatUtils.max(valuesArray))).append("|");
        stats.append("dev=").append(stdDev.evaluate(valuesArray)).append("|");
        stats.append("mode=").append(StatUtils.mode(valuesArray)[0]).append("|");
        stats.append("mean=").append(StatUtils.mean(valuesArray)).append("|");
        stats.append("median=").append(StatUtils.geometricMean(valuesArray)).append("|");
        stats.append("variance=").append(StatUtils.variance(valuesArray)).append("|");
        return stats.toString();
    }

}

package org.librairy.service.modeler.service;

import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import org.apache.commons.math3.stat.StatUtils;
import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

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
        if (valuesArray == null || valuesArray.length == 0 ) return "Empty Stats";
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


    public static Double entropy(List<Double> vector){

        Double sum = -vector.stream().map(val -> val*Math.log(val)).reduce((a,b) -> a+b).get();

        Double vocabSize = Double.valueOf(vector.size());

        Double entropy = sum / Math.log(vocabSize);

        return entropy;
    }


    public static void main(String[] args) {

        List<Double> vector = Arrays.asList(0.04166666666666666, 0.04166666666666666, 0.02777777777777777, 0.02777777777777777, 0.02777777777777777, 0.02777777777777777, 0.02777777777777777, 0.02777777777777777, 0.02777777777777777, 0.02777777777777777, 0.02777777777777777, 0.02777777777777777, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.01388888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888, 0.013888888888888888);
//        List<Double> vector = Arrays.asList(0.1,0.2);

        System.out.println(entropy(vector));
    }
}

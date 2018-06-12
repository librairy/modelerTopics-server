package cc.mallet.topics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Badenes Olmedo, Carlos <cbadenes@fi.upm.es>
 */
public class ModelParams implements Serializable{

    Double alpha            = 0.1;

    Double beta             = 0.001;

    Integer numTopics       = 10;

    Integer numTopWords     = 50;

    Integer numIterations   = 1000;

    Integer numRetries      = 10;

    String corpusFile;

    String outputDir;

    String language         = "en";

    String regEx            = "(.*);;(.*);;(.*);;(.*)";

    Integer textIndex       = 4;

    Integer labelIndex      = 3;

    Integer idIndex         = 1;

    String pos              = "VERB NOUN ADVERB ADJECTIVE";

    Boolean entities        = false;

    List<String> stopwords  = new ArrayList<>();

    public ModelParams(String corpusFile, String outputDir) {
        this.corpusFile = corpusFile;
        this.outputDir = outputDir;
    }

    public ModelParams() {
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public Double getAlpha() {
        return alpha;
    }

    public void setAlpha(Double alpha) {
        this.alpha = alpha;
    }

    public Double getBeta() {
        return beta;
    }

    public void setBeta(Double beta) {
        this.beta = beta;
    }

    public Integer getNumTopics() {
        return numTopics;
    }

    public void setNumTopics(Integer numTopics) {
        this.numTopics = numTopics;
    }

    public Integer getNumTopWords() {
        return numTopWords;
    }

    public void setNumTopWords(Integer numTopWords) {
        this.numTopWords = numTopWords;
    }

    public Integer getNumIterations() {
        return numIterations;
    }

    public void setNumIterations(Integer numIterations) {
        this.numIterations = numIterations;
    }

    public String getCorpusFile() {
        return corpusFile;
    }

    public void setCorpusFile(String corpusFile) {
        this.corpusFile = corpusFile;
    }

    public String getOutputDir() {
        return outputDir;
    }

    public void setOutputDir(String outputDir) {
        this.outputDir = outputDir;
    }

    public String getRegEx() {
        return regEx;
    }

    public void setRegEx(String regEx) {
        this.regEx = regEx;
    }

    public Integer getTextIndex() {
        return textIndex;
    }

    public void setTextIndex(Integer textIndex) {
        this.textIndex = textIndex;
    }

    public Integer getLabelIndex() {
        return labelIndex;
    }

    public void setLabelIndex(Integer labelIndex) {
        this.labelIndex = labelIndex;
    }

    public Integer getIdIndex() {
        return idIndex;
    }

    public void setIdIndex(Integer idIndex) {
        this.idIndex = idIndex;
    }

    public String getPos() {
        return pos;
    }

    public void setPos(String pos) {
        this.pos = pos;
    }

    public Integer getNumRetries() {
        return numRetries;
    }

    public void setNumRetries(Integer numRetries) {
        this.numRetries = numRetries;
    }

    public List<String> getStopwords() {
        return stopwords;
    }

    public void setStopwords(List<String> stopwords) {
        this.stopwords = stopwords;
    }

    public Boolean getEntities() {
        return entities;
    }

    public void setEntities(Boolean entities) {
        this.entities = entities;
    }

    @Override
    public String toString() {
        return "ModelParams{" +
                "alpha=" + alpha +
                ", beta=" + beta +
                ", numTopics=" + numTopics +
                ", numTopWords=" + numTopWords +
                ", numIterations=" + numIterations +
                ", numRetries=" + numRetries +
                ", corpusFile='" + corpusFile + '\'' +
                ", outputDir='" + outputDir + '\'' +
                ", language='" + language + '\'' +
                ", regEx='" + regEx + '\'' +
                ", textIndex=" + textIndex +
                ", labelIndex=" + labelIndex +
                ", idIndex=" + idIndex +
                ", pos='" + pos + '\'' +
                ", entities=" + entities +
                ", stopwords=" + stopwords +
                '}';
    }
}

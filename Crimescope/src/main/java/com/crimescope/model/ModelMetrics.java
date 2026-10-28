package com.crimescope.model;

import java.util.List;

public class ModelMetrics {
    private double accuracy;
    private double precisionMacro;
    private double recallMacro;
    private double f1Macro;
    private double precisionWeighted;
    private double recallWeighted;
    private double f1Weighted;
    private int[][] confusionMatrix;
    private List<FeatureImportance> featureImportance;
    private int testSampleCount;
    private int trainSampleCount;

    public ModelMetrics() {}

    public double getAccuracy() { return accuracy; }
    public void setAccuracy(double accuracy) { this.accuracy = accuracy; }

    public double getPrecisionMacro() { return precisionMacro; }
    public void setPrecisionMacro(double precisionMacro) { this.precisionMacro = precisionMacro; }

    public double getRecallMacro() { return recallMacro; }
    public void setRecallMacro(double recallMacro) { this.recallMacro = recallMacro; }

    public double getF1Macro() { return f1Macro; }
    public void setF1Macro(double f1Macro) { this.f1Macro = f1Macro; }

    public double getPrecisionWeighted() { return precisionWeighted; }
    public void setPrecisionWeighted(double precisionWeighted) { this.precisionWeighted = precisionWeighted; }

    public double getRecallWeighted() { return recallWeighted; }
    public void setRecallWeighted(double recallWeighted) { this.recallWeighted = recallWeighted; }

    public double getF1Weighted() { return f1Weighted; }
    public void setF1Weighted(double f1Weighted) { this.f1Weighted = f1Weighted; }

    public int[][] getConfusionMatrix() { return confusionMatrix; }
    public void setConfusionMatrix(int[][] confusionMatrix) { this.confusionMatrix = confusionMatrix; }

    public List<FeatureImportance> getFeatureImportance() { return featureImportance; }
    public void setFeatureImportance(List<FeatureImportance> featureImportance) { this.featureImportance = featureImportance; }

    public int getTestSampleCount() { return testSampleCount; }
    public void setTestSampleCount(int testSampleCount) { this.testSampleCount = testSampleCount; }

    public int getTrainSampleCount() { return trainSampleCount; }
    public void setTrainSampleCount(int trainSampleCount) { this.trainSampleCount = trainSampleCount; }
}

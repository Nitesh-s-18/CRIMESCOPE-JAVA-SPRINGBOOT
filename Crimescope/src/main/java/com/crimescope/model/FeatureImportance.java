package com.crimescope.model;

public class FeatureImportance {
    private String feature;
    private double importance;

    public FeatureImportance() {}

    public FeatureImportance(String feature, double importance) {
        this.feature = feature;
        this.importance = importance;
    }

    public String getFeature() { return feature; }
    public void setFeature(String feature) { this.feature = feature; }

    public double getImportance() { return importance; }
    public void setImportance(double importance) { this.importance = importance; }
}

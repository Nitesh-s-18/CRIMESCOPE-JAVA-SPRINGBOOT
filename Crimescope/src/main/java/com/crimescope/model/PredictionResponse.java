package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;

public class PredictionResponse {
    @JsonProperty("risk_label")
    private String riskLabel;

    @JsonProperty("risk_color")
    private String riskColor;

    @JsonProperty("probabilities")
    private Map<String, Double> probabilities;

    @JsonProperty("confidence")
    private double confidence;

    @JsonProperty("historical_baseline_score")
    private double historicalBaselineScore;

    @JsonProperty("historical_rating")
    private String historicalRating;

    @JsonProperty("inputs")
    private Map<String, Object> inputs;

    @JsonProperty("feature_chart")
    private String featureChart;

    public PredictionResponse() {}

    public String getRiskLabel() { return riskLabel; }
    public void setRiskLabel(String riskLabel) { this.riskLabel = riskLabel; }

    public String getRiskColor() { return riskColor; }
    public void setRiskColor(String riskColor) { this.riskColor = riskColor; }

    public Map<String, Double> getProbabilities() { return probabilities; }
    public void setProbabilities(Map<String, Double> probabilities) { this.probabilities = probabilities; }

    public double getConfidence() { return confidence; }
    public void setConfidence(double confidence) { this.confidence = confidence; }

    public double getHistoricalBaselineScore() { return historicalBaselineScore; }
    public void setHistoricalBaselineScore(double historicalBaselineScore) { this.historicalBaselineScore = historicalBaselineScore; }

    public String getHistoricalRating() { return historicalRating; }
    public void setHistoricalRating(String historicalRating) { this.historicalRating = historicalRating; }

    public Map<String, Object> getInputs() { return inputs; }
    public void setInputs(Map<String, Object> inputs) { this.inputs = inputs; }

    public String getFeatureChart() { return featureChart; }
    public void setFeatureChart(String featureChart) { this.featureChart = featureChart; }
}

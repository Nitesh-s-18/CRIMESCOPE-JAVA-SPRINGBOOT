package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HotspotRequest {
    @JsonProperty("eps_km")
    private double epsKm = 0.60;

    @JsonProperty("min_samples")
    private int minSamples = 25;

    @JsonProperty("color_by")
    private String colorBy = "crime_type";

    public HotspotRequest() {}

    public double getEpsKm() { return epsKm; }
    public void setEpsKm(double epsKm) { this.epsKm = epsKm; }

    public int getMinSamples() { return minSamples; }
    public void setMinSamples(int minSamples) { this.minSamples = minSamples; }

    public String getColorBy() { return colorBy; }
    public void setColorBy(String colorBy) { this.colorBy = colorBy; }
}

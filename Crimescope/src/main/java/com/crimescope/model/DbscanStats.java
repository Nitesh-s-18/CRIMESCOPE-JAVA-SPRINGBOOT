package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DbscanStats {
    @JsonProperty("num_clusters")
    private int numClusters;

    @JsonProperty("hotspot_crimes")
    private int hotspotCrimes;

    @JsonProperty("noise_crimes")
    private int noiseCrimes;

    @JsonProperty("noise_pct")
    private double noisePct;

    @JsonProperty("eps_km")
    private double epsKm;

    @JsonProperty("min_samples")
    private int minSamples;

    public DbscanStats() {}

    public DbscanStats(int numClusters, int hotspotCrimes, int noiseCrimes, double noisePct, double epsKm, int minSamples) {
        this.numClusters = numClusters;
        this.hotspotCrimes = hotspotCrimes;
        this.noiseCrimes = noiseCrimes;
        this.noisePct = noisePct;
        this.epsKm = epsKm;
        this.minSamples = minSamples;
    }

    public int getNumClusters() { return numClusters; }
    public void setNumClusters(int numClusters) { this.numClusters = numClusters; }

    public int getHotspotCrimes() { return hotspotCrimes; }
    public void setHotspotCrimes(int hotspotCrimes) { this.hotspotCrimes = hotspotCrimes; }

    public int getNoiseCrimes() { return noiseCrimes; }
    public void setNoiseCrimes(int noiseCrimes) { this.noiseCrimes = noiseCrimes; }

    public double getNoisePct() { return noisePct; }
    public void setNoisePct(double noisePct) { this.noisePct = noisePct; }

    public double getEpsKm() { return epsKm; }
    public void setEpsKm(double epsKm) { this.epsKm = epsKm; }

    public int getMinSamples() { return minSamples; }
    public void setMinSamples(int minSamples) { this.minSamples = minSamples; }
}

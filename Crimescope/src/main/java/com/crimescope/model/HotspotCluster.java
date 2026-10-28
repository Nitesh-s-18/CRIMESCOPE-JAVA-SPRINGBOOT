package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HotspotCluster {
    @JsonProperty("Cluster ID")
    private String clusterIdStr;

    @JsonProperty("Raw Cluster ID")
    private int rawClusterId;

    @JsonProperty("Total Incidents")
    private int totalIncidents;

    @JsonProperty("% of All Crimes")
    private double percentOfAllCrimes;

    @JsonProperty("Primary Area")
    private String primaryArea;

    @JsonProperty("Dominant Crime Type")
    private String dominantCrimeType;

    @JsonProperty("Primary Location")
    private String primaryLocation;

    @JsonProperty("Centroid Latitude")
    private double centroidLatitude;

    @JsonProperty("Centroid Longitude")
    private double centroidLongitude;

    public HotspotCluster() {}

    public HotspotCluster(String clusterIdStr, int rawClusterId, int totalIncidents, double percentOfAllCrimes,
                          String primaryArea, String dominantCrimeType, String primaryLocation,
                          double centroidLatitude, double centroidLongitude) {
        this.clusterIdStr = clusterIdStr;
        this.rawClusterId = rawClusterId;
        this.totalIncidents = totalIncidents;
        this.percentOfAllCrimes = percentOfAllCrimes;
        this.primaryArea = primaryArea;
        this.dominantCrimeType = dominantCrimeType;
        this.primaryLocation = primaryLocation;
        this.centroidLatitude = centroidLatitude;
        this.centroidLongitude = centroidLongitude;
    }

    public String getClusterIdStr() { return clusterIdStr; }
    public void setClusterIdStr(String clusterIdStr) { this.clusterIdStr = clusterIdStr; }

    public int getRawClusterId() { return rawClusterId; }
    public void setRawClusterId(int rawClusterId) { this.rawClusterId = rawClusterId; }

    public int getTotalIncidents() { return totalIncidents; }
    public void setTotalIncidents(int totalIncidents) { this.totalIncidents = totalIncidents; }

    public double getPercentOfAllCrimes() { return percentOfAllCrimes; }
    public void setPercentOfAllCrimes(double percentOfAllCrimes) { this.percentOfAllCrimes = percentOfAllCrimes; }

    public String getPrimaryArea() { return primaryArea; }
    public void setPrimaryArea(String primaryArea) { this.primaryArea = primaryArea; }

    public String getDominantCrimeType() { return dominantCrimeType; }
    public void setDominantCrimeType(String dominantCrimeType) { this.dominantCrimeType = dominantCrimeType; }

    public String getPrimaryLocation() { return primaryLocation; }
    public void setPrimaryLocation(String primaryLocation) { this.primaryLocation = primaryLocation; }

    public double getCentroidLatitude() { return centroidLatitude; }
    public void setCentroidLatitude(double centroidLatitude) { this.centroidLatitude = centroidLatitude; }

    public double getCentroidLongitude() { return centroidLongitude; }
    public void setCentroidLongitude(double centroidLongitude) { this.centroidLongitude = centroidLongitude; }
}

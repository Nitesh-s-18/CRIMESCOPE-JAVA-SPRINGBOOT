package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AreaRanking {
    @JsonProperty("Rank")
    private int rank;

    @JsonProperty("area")
    private String area;

    @JsonProperty("Total_Crimes")
    private int totalCrimes;

    @JsonProperty("% Share")
    private double percentShare;

    @JsonProperty("Top_Crime_Type")
    private String topCrimeType;

    @JsonProperty("Avg_Victim_Age")
    private double avgVictimAge;

    @JsonProperty("Latitude")
    private double latitude;

    @JsonProperty("Longitude")
    private double longitude;

    public AreaRanking() {}

    public AreaRanking(int rank, String area, int totalCrimes, double percentShare,
                       String topCrimeType, double avgVictimAge, double latitude, double longitude) {
        this.rank = rank;
        this.area = area;
        this.totalCrimes = totalCrimes;
        this.percentShare = percentShare;
        this.topCrimeType = topCrimeType;
        this.avgVictimAge = avgVictimAge;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public int getTotalCrimes() { return totalCrimes; }
    public void setTotalCrimes(int totalCrimes) { this.totalCrimes = totalCrimes; }

    public double getPercentShare() { return percentShare; }
    public void setPercentShare(double percentShare) { this.percentShare = percentShare; }

    public String getTopCrimeType() { return topCrimeType; }
    public void setTopCrimeType(String topCrimeType) { this.topCrimeType = topCrimeType; }

    public double getAvgVictimAge() { return avgVictimAge; }
    public void setAvgVictimAge(double avgVictimAge) { this.avgVictimAge = avgVictimAge; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }
}

package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FilterResponse {
    @JsonProperty("total_records")
    private int totalRecords;

    @JsonProperty("num_hotspots")
    private int numHotspots;

    @JsonProperty("highest_risk_area")
    private String highestRiskArea;

    @JsonProperty("most_common_crime")
    private String mostCommonCrime;

    @JsonProperty("most_common_count")
    private int mostCommonCount;

    @JsonProperty("active_label")
    private String activeLabel;

    @JsonProperty("chart_category")
    private String chartCategory;

    @JsonProperty("chart_timeline")
    private String chartTimeline;

    @JsonProperty("chart_hourly")
    private String chartHourly;

    @JsonProperty("chart_areas")
    private String chartAreas;

    @JsonProperty("chart_map")
    private String chartMap;

    public FilterResponse() {}

    public int getTotalRecords() { return totalRecords; }
    public void setTotalRecords(int totalRecords) { this.totalRecords = totalRecords; }

    public int getNumHotspots() { return numHotspots; }
    public void setNumHotspots(int numHotspots) { this.numHotspots = numHotspots; }

    public String getHighestRiskArea() { return highestRiskArea; }
    public void setHighestRiskArea(String highestRiskArea) { this.highestRiskArea = highestRiskArea; }

    public String getMostCommonCrime() { return mostCommonCrime; }
    public void setMostCommonCrime(String mostCommonCrime) { this.mostCommonCrime = mostCommonCrime; }

    public int getMostCommonCount() { return mostCommonCount; }
    public void setMostCommonCount(int mostCommonCount) { this.mostCommonCount = mostCommonCount; }

    public String getActiveLabel() { return activeLabel; }
    public void setActiveLabel(String activeLabel) { this.activeLabel = activeLabel; }

    public String getChartCategory() { return chartCategory; }
    public void setChartCategory(String chartCategory) { this.chartCategory = chartCategory; }

    public String getChartTimeline() { return chartTimeline; }
    public void setChartTimeline(String chartTimeline) { this.chartTimeline = chartTimeline; }

    public String getChartHourly() { return chartHourly; }
    public void setChartHourly(String chartHourly) { this.chartHourly = chartHourly; }

    public String getChartAreas() { return chartAreas; }
    public void setChartAreas(String chartAreas) { this.chartAreas = chartAreas; }

    public String getChartMap() { return chartMap; }
    public void setChartMap(String chartMap) { this.chartMap = chartMap; }
}

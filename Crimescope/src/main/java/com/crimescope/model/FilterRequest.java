package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class FilterRequest {
    @JsonProperty("mode")
    private String mode = "all";

    @JsonProperty("exact_date")
    private String exactDate;

    @JsonProperty("start_date")
    private String startDate;

    @JsonProperty("end_date")
    private String endDate;

    @JsonProperty("year")
    private String year = "ALL";

    @JsonProperty("month")
    private String month = "ALL";

    @JsonProperty("area")
    private String area = "ALL";

    @JsonProperty("crime_type")
    private String crimeType = "ALL";

    public FilterRequest() {}

    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }

    public String getExactDate() { return exactDate; }
    public void setExactDate(String exactDate) { this.exactDate = exactDate; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public String getEndDate() { return endDate; }
    public void setEndDate(String endDate) { this.endDate = endDate; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getCrimeType() { return crimeType; }
    public void setCrimeType(String crimeType) { this.crimeType = crimeType; }
}

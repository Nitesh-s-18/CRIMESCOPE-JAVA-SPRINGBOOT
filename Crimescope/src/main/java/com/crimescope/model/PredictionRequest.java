package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PredictionRequest {
    @JsonProperty("area")
    private String area = "Gandhipuram";

    @JsonProperty("crime_type")
    private String crimeType = "Theft";

    @JsonProperty("location_type")
    private String locationType = "Commercial Area";

    @JsonProperty("day_of_week")
    private String dayOfWeek = "Friday";

    @JsonProperty("hour")
    private int hour = 21;

    @JsonProperty("month")
    private int month = 10;

    public PredictionRequest() {}

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public String getCrimeType() { return crimeType; }
    public void setCrimeType(String crimeType) { this.crimeType = crimeType; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }
}

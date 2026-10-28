package com.crimescope.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class CrimeRecord {
    private String crimeId;
    private String date;
    private String time;
    private String city;
    private String area;
    private double latitude;
    private double longitude;
    private String crimeType;
    private int victimAge;
    private String locationType;
    private String dayOfWeek;
    private int hour;
    private int month;

    // Derived / Preprocessed Features
    private LocalDateTime datetime;
    private int dayOfMonth;
    private int quarter;
    private int year;
    private int isWeekend;
    private String timeSlot;
    private String season;
    private double hourSin;
    private double hourCos;
    private double monthSin;
    private double monthCos;
    private double daySin;
    private double dayCos;

    // Hotspot cluster assignment
    private int clusterId = -1;
    private boolean isHotspot = false;

    // Risk target label for training (0=Low, 1=Medium, 2=High)
    private int targetRisk = 1;

    public CrimeRecord() {}

    public CrimeRecord(String crimeId, String date, String time, String city, String area,
                       double latitude, double longitude, String crimeType, int victimAge,
                       String locationType, String dayOfWeek, int hour, int month) {
        this.crimeId = crimeId;
        this.date = date;
        this.time = time;
        this.city = city;
        this.area = area;
        this.latitude = latitude;
        this.longitude = longitude;
        this.crimeType = crimeType;
        this.victimAge = victimAge;
        this.locationType = locationType;
        this.dayOfWeek = dayOfWeek;
        this.hour = hour;
        this.month = month;
    }

    // Getters and Setters
    public String getCrimeId() { return crimeId; }
    public void setCrimeId(String crimeId) { this.crimeId = crimeId; }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    public String getArea() { return area; }
    public void setArea(String area) { this.area = area; }

    public double getLatitude() { return latitude; }
    public void setLatitude(double latitude) { this.latitude = latitude; }

    public double getLongitude() { return longitude; }
    public void setLongitude(double longitude) { this.longitude = longitude; }

    public String getCrimeType() { return crimeType; }
    public void setCrimeType(String crimeType) { this.crimeType = crimeType; }

    public int getVictimAge() { return victimAge; }
    public void setVictimAge(int victimAge) { this.victimAge = victimAge; }

    public String getLocationType() { return locationType; }
    public void setLocationType(String locationType) { this.locationType = locationType; }

    public String getDayOfWeek() { return dayOfWeek; }
    public void setDayOfWeek(String dayOfWeek) { this.dayOfWeek = dayOfWeek; }

    public int getHour() { return hour; }
    public void setHour(int hour) { this.hour = hour; }

    public int getMonth() { return month; }
    public void setMonth(int month) { this.month = month; }

    public LocalDateTime getDatetime() { return datetime; }
    public void setDatetime(LocalDateTime datetime) { this.datetime = datetime; }

    public int getDayOfMonth() { return dayOfMonth; }
    public void setDayOfMonth(int dayOfMonth) { this.dayOfMonth = dayOfMonth; }

    public int getQuarter() { return quarter; }
    public void setQuarter(int quarter) { this.quarter = quarter; }

    public int getYear() { return year; }
    public void setYear(int year) { this.year = year; }

    public int getIsWeekend() { return isWeekend; }
    public void setIsWeekend(int isWeekend) { this.isWeekend = isWeekend; }

    public String getTimeSlot() { return timeSlot; }
    public void setTimeSlot(String timeSlot) { this.timeSlot = timeSlot; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }

    public double getHourSin() { return hourSin; }
    public void setHourSin(double hourSin) { this.hourSin = hourSin; }

    public double getHourCos() { return hourCos; }
    public void setHourCos(double hourCos) { this.hourCos = hourCos; }

    public double getMonthSin() { return monthSin; }
    public void setMonthSin(double monthSin) { this.monthSin = monthSin; }

    public double getMonthCos() { return monthCos; }
    public void setMonthCos(double monthCos) { this.monthCos = monthCos; }

    public double getDaySin() { return daySin; }
    public void setDaySin(double daySin) { this.daySin = daySin; }

    public double getDayCos() { return dayCos; }
    public void setDayCos(double dayCos) { this.dayCos = dayCos; }

    public int getClusterId() { return clusterId; }
    public void setClusterId(int clusterId) { this.clusterId = clusterId; }

    public boolean isHotspot() { return isHotspot; }
    public void setHotspot(boolean hotspot) { isHotspot = hotspot; }

    public int getTargetRisk() { return targetRisk; }
    public void setTargetRisk(int targetRisk) { this.targetRisk = targetRisk; }
}

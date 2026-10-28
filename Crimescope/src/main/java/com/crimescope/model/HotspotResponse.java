package com.crimescope.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class HotspotResponse {
    @JsonProperty("stats")
    private DbscanStats stats;

    @JsonProperty("map_html")
    private String mapHtml;

    @JsonProperty("table_html")
    private String tableHtml;

    public HotspotResponse() {}

    public HotspotResponse(DbscanStats stats, String mapHtml, String tableHtml) {
        this.stats = stats;
        this.mapHtml = mapHtml;
        this.tableHtml = tableHtml;
    }

    public DbscanStats getStats() { return stats; }
    public void setStats(DbscanStats stats) { this.stats = stats; }

    public String getMapHtml() { return mapHtml; }
    public void setMapHtml(String mapHtml) { this.mapHtml = mapHtml; }

    public String getTableHtml() { return tableHtml; }
    public void setTableHtml(String tableHtml) { this.tableHtml = tableHtml; }
}

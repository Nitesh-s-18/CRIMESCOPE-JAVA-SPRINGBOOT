package com.crimescope.service;

import com.crimescope.model.AreaRanking;
import com.crimescope.model.CrimeRecord;
import com.crimescope.model.FeatureImportance;
import com.crimescope.model.HotspotCluster;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AnalyticsService {

    public static final Map<String, String> CRIME_TYPE_COLORS = Map.of(
            "Theft", "#38bdf8",
            "Burglary", "#f59e0b",
            "Robbery", "#ef4444",
            "Assault", "#ec4899",
            "Vehicle Theft", "#a855f7",
            "Fraud", "#10b981",
            "Vandalism", "#f97316"
    );

    private static String escapeJs(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ");
    }

    private static String wrapPlotly(String divId, String dataJson, String layoutJson, int height) {
        return String.format(
                "<div style=\"width:100%%; min-height:%dpx;\">" +
                "<div id=\"%s\" class=\"plotly-graph-div\" style=\"height:%dpx; width:100%%;\"></div>" +
                "<script type=\"text/javascript\">" +
                "  try { Plotly.newPlot('%s', %s, %s, {responsive: true, displayModeBar: false}); } catch(e){ console.error(e); }" +
                "</script>" +
                "</div>",
                height, divId, height, divId, dataJson, layoutJson
        );
    }

    public String plotCrimeCategoryDistribution(List<CrimeRecord> records) {
        String divId = "cat_donut_" + System.nanoTime();
        if (records == null || records.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No incidents in active filter.</div>";
        }

        Map<String, Long> counts = records.stream()
                .collect(Collectors.groupingBy(CrimeRecord::getCrimeType, Collectors.counting()));

        StringBuilder labels = new StringBuilder("[");
        StringBuilder values = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");

        int i = 0;
        for (Map.Entry<String, Long> e : counts.entrySet()) {
            if (i > 0) { labels.append(","); values.append(","); colors.append(","); }
            labels.append("\"").append(escapeJs(e.getKey())).append("\"");
            values.append(e.getValue());
            colors.append("\"").append(CRIME_TYPE_COLORS.getOrDefault(e.getKey(), "#94a3b8")).append("\"");
            i++;
        }
        labels.append("]");
        values.append("]");
        colors.append("]");

        String dataJson = String.format("""
            [{
                "labels": %s,
                "values": %s,
                "type": "pie",
                "hole": 0.55,
                "marker": { "colors": %s, "line": { "color": "#0f172a", "width": 2 } },
                "textinfo": "label+percent",
                "hoverinfo": "label+value+percent",
                "hovertemplate": "<b>%%{label}</b><br>Incidents: %%{value:,}<br>Share: %%{percent}<extra></extra>"
            }]
        """, labels, values, colors);

        String layoutJson = String.format("""
            {
                "template": "plotly_dark",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 360,
                "margin": { "l": 20, "r": 20, "t": 55, "b": 25 },
                "title": {
                    "text": "<b>Crime Incidents by Category</b><br><span style='font-size:11.5px;color:#94a3b8'>Proportional share of total recorded incidents across categories</span>",
                    "x": 0.03, "xanchor": "left"
                },
                "annotations": [{
                    "text": "<b>%d</b><br><span style='font-size:11px;color:#94a3b8'>Total Crimes</span>",
                    "x": 0.5, "y": 0.5, "font": { "size": 15, "color": "#e2e8f0" }, "showarrow": false
                }],
                "legend": { "bgcolor": "rgba(15,23,42,0.6)", "bordercolor": "rgba(255,255,255,0.1)", "borderwidth": 1 }
            }
        """, records.size());

        return wrapPlotly(divId, dataJson, layoutJson, 360);
    }

    public String plotCrimeTrendsTimeline(List<CrimeRecord> filtered, List<CrimeRecord> full) {
        String divId = "timeline_" + System.nanoTime();
        if (filtered == null || filtered.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No timeline data matching active filter.</div>";
        }

        // Check if viewing full dataset or multi-month range
        Set<String> distinctDates = filtered.stream().map(CrimeRecord::getDate).collect(Collectors.toSet());
        boolean isMacroView = distinctDates.size() > 60;

        StringBuilder xVals = new StringBuilder("[");
        StringBuilder yVals = new StringBuilder("[");
        StringBuilder customData = new StringBuilder("[");

        if (isMacroView) {
            // Group by Year-Month (e.g. 2023-01)
            Map<String, Long> byMonth = new TreeMap<>();
            for (CrimeRecord r : filtered) {
                String ym = r.getDate().substring(0, 7);
                byMonth.put(ym, byMonth.getOrDefault(ym, 0L) + 1);
            }

            int idx = 0;
            for (Map.Entry<String, Long> e : byMonth.entrySet()) {
                if (idx > 0) { xVals.append(","); yVals.append(","); customData.append(","); }
                String ym = e.getKey();
                String[] p = ym.split("-");
                xVals.append("\"").append(ym).append("\"");
                yVals.append(e.getValue());
                // customdata format: [x, y, label, year, month]
                customData.append(String.format("[\"%s\", %d, \"%s\", \"%s\", \"%d\"]", ym, e.getValue(), ym, p[0], Integer.parseInt(p[1])));
                idx++;
            }
        } else {
            // Group by exact day
            Map<String, Long> byDay = new TreeMap<>();
            for (CrimeRecord r : filtered) {
                byDay.put(r.getDate(), byDay.getOrDefault(r.getDate(), 0L) + 1);
            }

            int idx = 0;
            for (Map.Entry<String, Long> e : byDay.entrySet()) {
                if (idx > 0) { xVals.append(","); yVals.append(","); customData.append(","); }
                xVals.append("\"").append(e.getKey()).append("\"");
                yVals.append(e.getValue());
                customData.append("\"").append(e.getKey()).append("\"");
                idx++;
            }
        }

        xVals.append("]");
        yVals.append("]");
        customData.append("]");

        String dataJson = String.format("""
            [{
                "x": %s,
                "y": %s,
                "customdata": %s,
                "type": "scatter",
                "mode": "lines+markers",
                "line": { "color": "#38bdf8", "width": 3, "shape": "spline" },
                "marker": { "size": 6, "color": "#38bdf8", "line": { "color": "#0f172a", "width": 1 } },
                "fill": "tozeroy",
                "fillcolor": "rgba(56, 189, 248, 0.12)",
                "hovertemplate": "<b>Date / Period:</b> %%{x}<br><b>Incidents:</b> %%{y:,}<extra></extra>"
            }]
        """, xVals, yVals, customData);

        String titleText = isMacroView ?
                "<b>Crime Incident Monitoring Timeline</b><br><span style='font-size:11.5px;color:#94a3b8'>Month-by-month incident trajectory across 2023–2024</span>" :
                "<b>Daily Incident Surveillance Timeline</b><br><span style='font-size:11.5px;color:#94a3b8'>Granular day-by-day recorded incident counts</span>";

        String layoutJson = String.format("""
            {
                "template": "plotly_dark",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 360,
                "margin": { "l": 45, "r": 25, "t": 60, "b": 45 },
                "title": { "text": "%s", "x": 0.03, "xanchor": "left" },
                "xaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" },
                "yaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0", "title": "Incident Volume" }
            }
        """, titleText);

        return wrapPlotly(divId, dataJson, layoutJson, 360);
    }

    public String plotHourlyDistribution(List<CrimeRecord> records) {
        String divId = "hourly_" + System.nanoTime();
        if (records == null || records.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No hourly data matching active filter.</div>";
        }

        int[] hourlyCounts = new int[24];
        for (CrimeRecord r : records) {
            int h = r.getHour();
            if (h >= 0 && h < 24) hourlyCounts[h]++;
        }

        int maxCount = Arrays.stream(hourlyCounts).max().orElse(1);
        int q75 = (int) (maxCount * 0.75);

        StringBuilder xVals = new StringBuilder("[");
        StringBuilder yVals = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");

        for (int h = 0; h < 24; h++) {
            if (h > 0) { xVals.append(","); yVals.append(","); colors.append(","); }
            String label = String.format("%02d:00", h);
            xVals.append("\"").append(label).append("\"");
            yVals.append(hourlyCounts[h]);

            if (hourlyCounts[h] >= q75) {
                colors.append("\"#ef4444\""); // Crimson Peak
            } else if (hourlyCounts[h] >= maxCount * 0.40) {
                colors.append("\"#f59e0b\""); // Amber Medium
            } else {
                colors.append("\"#38bdf8\""); // Sky Blue Low
            }
        }
        xVals.append("]");
        yVals.append("]");
        colors.append("]");

        String dataJson = String.format("""
            [{
                "x": %s,
                "y": %s,
                "type": "bar",
                "marker": { "color": %s, "line": { "color": "rgba(255,255,255,0.1)", "width": 1 } },
                "hovertemplate": "<b>Hour:</b> %%{x}<br><b>Incidents:</b> %%{y:,}<extra></extra>"
            }]
        """, xVals, yVals, colors);

        String layoutJson = """
            {
                "template": "plotly_dark",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 360,
                "margin": { "l": 45, "r": 25, "t": 60, "b": 45 },
                "title": {
                    "text": "<b>24-Hour Crime Activity Profile</b><br><span style='font-size:11.5px;color:#94a3b8'>Peak danger windows highlighted in red; moderate activity in amber</span>",
                    "x": 0.03, "xanchor": "left"
                },
                "xaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" },
                "yaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0", "title": "Crime Count" }
            }
        """;

        return wrapPlotly(divId, dataJson, layoutJson, 360);
    }

    public String plotTopAreasBar(List<CrimeRecord> records, int topN) {
        String divId = "areas_" + System.nanoTime();
        if (records == null || records.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No area data matching active filter.</div>";
        }

        Map<String, Long> byArea = records.stream()
                .collect(Collectors.groupingBy(CrimeRecord::getArea, Collectors.counting()));

        List<Map.Entry<String, Long>> sorted = byArea.entrySet().stream()
                .sorted(Map.Entry.comparingByValue())
                .collect(Collectors.toList());

        if (sorted.size() > topN) {
            sorted = sorted.subList(sorted.size() - topN, sorted.size());
        }

        StringBuilder xVals = new StringBuilder("[");
        StringBuilder yVals = new StringBuilder("[");

        for (int i = 0; i < sorted.size(); i++) {
            if (i > 0) { xVals.append(","); yVals.append(","); }
            xVals.append(sorted.get(i).getValue());
            yVals.append("\"").append(escapeJs(sorted.get(i).getKey())).append("\"");
        }
        xVals.append("]");
        yVals.append("]");

        String dataJson = String.format("""
            [{
                "x": %s,
                "y": %s,
                "type": "bar",
                "orientation": "h",
                "marker": { "color": "#38bdf8", "line": { "color": "#0f172a", "width": 1 } },
                "text": %s,
                "textposition": "auto",
                "hovertemplate": "<b>Area:</b> %%{y}<br><b>Total Crimes:</b> %%{x:,}<extra></extra>"
            }]
        """, xVals, yVals, xVals);

        String layoutJson = """
            {
                "template": "plotly_dark",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 360,
                "margin": { "l": 120, "r": 30, "t": 60, "b": 45 },
                "title": {
                    "text": "<b>Top Active Neighborhoods by Crime Volume</b><br><span style='font-size:11.5px;color:#94a3b8'>Ranking beats by cumulative incident density</span>",
                    "x": 0.03, "xanchor": "left"
                },
                "xaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" },
                "yaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" }
            }
        """;

        return wrapPlotly(divId, dataJson, layoutJson, 360);
    }

    public String plotHotspotMap(List<CrimeRecord> records, List<HotspotCluster> clusters, String colorBy) {
        String divId = "hotspot_map_" + System.nanoTime();
        if (records == null || records.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No coordinates to map.</div>";
        }

        // Incident markers
        StringBuilder lats = new StringBuilder("[");
        StringBuilder lons = new StringBuilder("[");
        StringBuilder colors = new StringBuilder("[");
        StringBuilder texts = new StringBuilder("[");

        for (int i = 0; i < records.size(); i++) {
            CrimeRecord r = records.get(i);
            if (i > 0) { lats.append(","); lons.append(","); colors.append(","); texts.append(","); }
            lats.append(r.getLatitude());
            lons.append(r.getLongitude());

            String c = "cluster_id".equalsIgnoreCase(colorBy) ?
                    (r.getClusterId() == -1 ? "#94a3b8" : "#38bdf8") :
                    CRIME_TYPE_COLORS.getOrDefault(r.getCrimeType(), "#38bdf8");
            colors.append("\"").append(c).append("\"");

            String desc = String.format("<b>%s</b> (%s)<br>Area: %s<br>Time: %s",
                    escapeJs(r.getCrimeType()), escapeJs(r.getLocationType()), escapeJs(r.getArea()), escapeJs(r.getTime()));
            texts.append("\"").append(desc).append("\"");
        }
        lats.append("]");
        lons.append("]");
        colors.append("]");
        texts.append("]");

        // Cluster Centroids markers
        StringBuilder cLats = new StringBuilder("[");
        StringBuilder cLons = new StringBuilder("[");
        StringBuilder cTexts = new StringBuilder("[");

        if (clusters != null) {
            for (int i = 0; i < clusters.size(); i++) {
                HotspotCluster c = clusters.get(i);
                if (i > 0) { cLats.append(","); cLons.append(","); cTexts.append(","); }
                cLats.append(c.getCentroidLatitude());
                cLons.append(c.getCentroidLongitude());
                String t = String.format("<b>%s</b><br>Incidents: %d (%s%%)<br>Top Area: %s<br>Top Offense: %s",
                        c.getClusterIdStr(), c.getTotalIncidents(), c.getPercentOfAllCrimes(), escapeJs(c.getPrimaryArea()), escapeJs(c.getDominantCrimeType()));
                cTexts.append("\"").append(t).append("\"");
            }
        }
        cLats.append("]");
        cLons.append("]");
        cTexts.append("]");

        String dataJson = String.format("""
            [
                {
                    "type": "scatter",
                    "mode": "markers",
                    "name": "Incidents",
                    "x": %s,
                    "y": %s,
                    "text": %s,
                    "marker": { "size": 6, "color": %s, "opacity": 0.75 },
                    "hovertemplate": "%%{text}<extra></extra>"
                },
                {
                    "type": "scatter",
                    "mode": "markers",
                    "name": "Hotspot Centroids",
                    "x": %s,
                    "y": %s,
                    "text": %s,
                    "marker": { "size": 24, "color": "rgba(239,68,68,0.85)", "symbol": "circle", "line": { "color": "#ffffff", "width": 2 } },
                    "hovertemplate": "%%{text}<extra></extra>"
                }
            ]
        """, lons, lats, texts, colors, cLons, cLats, cTexts);

        String layoutJson = """
            {
                "template": "plotly_dark",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 450,
                "margin": { "l": 45, "r": 25, "t": 60, "b": 45 },
                "title": {
                    "text": "<b>Geospatial Crime Hotspot Map (DBSCAN Clusters)</b><br><span style='font-size:11.5px;color:#94a3b8'>Pulsing red markers indicate discovered hotspot cluster centroids</span>",
                    "x": 0.03, "xanchor": "left"
                },
                "xaxis": { "title": "Longitude (°E)", "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" },
                "yaxis": { "title": "Latitude (°N)", "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" }
            }
        """;

        return wrapPlotly(divId, dataJson, layoutJson, 450);
    }

    public String plotDayHourHeatmap(List<CrimeRecord> records) {
        String divId = "heatmap_" + System.nanoTime();
        if (records == null || records.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No heatmap data.</div>";
        }

        String[] days = {"Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"};
        int[][] matrix = new int[7][24];

        for (CrimeRecord r : records) {
            String d = r.getDayOfWeek();
            int h = r.getHour();
            for (int i = 0; i < 7; i++) {
                if (days[i].equalsIgnoreCase(d)) {
                    if (h >= 0 && h < 24) matrix[i][h]++;
                    break;
                }
            }
        }

        StringBuilder zJson = new StringBuilder("[");
        for (int i = 0; i < 7; i++) {
            if (i > 0) zJson.append(",");
            zJson.append(Arrays.toString(matrix[i]));
        }
        zJson.append("]");

        StringBuilder xJson = new StringBuilder("[");
        for (int h = 0; h < 24; h++) {
            if (h > 0) xJson.append(",");
            xJson.append("\"").append(String.format("%02d:00", h)).append("\"");
        }
        xJson.append("]");

        String dataJson = String.format("""
            [{
                "z": %s,
                "x": %s,
                "y": ["Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday", "Sunday"],
                "type": "heatmap",
                "colorscale": [
                    [0.0, "#0f172a"],
                    [0.3, "#0284c7"],
                    [0.6, "#f59e0b"],
                    [1.0, "#ef4444"]
                ],
                "hovertemplate": "<b>%%{y} at %%{x}</b><br>Incidents: %%{z:,}<extra></extra>"
            }]
        """, zJson, xJson);

        String layoutJson = """
            {
                "template": "plotly_dark",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 400,
                "margin": { "l": 100, "r": 30, "t": 60, "b": 45 },
                "title": {
                    "text": "<b>Weekly Spatio-Temporal Matrix (Day vs. Hour)</b><br><span style='font-size:11.5px;color:#94a3b8'>Bright red and amber indicates severe crime intensity slots</span>",
                    "x": 0.03, "xanchor": "left"
                },
                "xaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" },
                "yaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" }
            }
        """;

        return wrapPlotly(divId, dataJson, layoutJson, 400);
    }

    public String plotLocationCrimeCrosstab(List<CrimeRecord> records) {
        String divId = "crosstab_" + System.nanoTime();
        if (records == null || records.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No location data.</div>";
        }

        List<String> locations = records.stream().map(CrimeRecord::getLocationType).distinct().sorted().toList();
        List<String> crimeTypes = records.stream().map(CrimeRecord::getCrimeType).distinct().sorted().toList();

        Map<String, Map<String, Integer>> table = new HashMap<>();
        for (CrimeRecord r : records) {
            table.computeIfAbsent(r.getCrimeType(), k -> new HashMap<>())
                    .merge(r.getLocationType(), 1, Integer::sum);
        }

        StringBuilder traces = new StringBuilder("[");
        for (int c = 0; c < crimeTypes.size(); c++) {
            String ct = crimeTypes.get(c);
            if (c > 0) traces.append(",");

            StringBuilder xArr = new StringBuilder("[");
            StringBuilder yArr = new StringBuilder("[");
            for (int l = 0; l < locations.size(); l++) {
                if (l > 0) { xArr.append(","); yArr.append(","); }
                String loc = locations.get(l);
                xArr.append("\"").append(escapeJs(loc)).append("\"");
                yArr.append(table.getOrDefault(ct, Collections.emptyMap()).getOrDefault(loc, 0));
            }
            xArr.append("]");
            yArr.append("]");

            String color = CRIME_TYPE_COLORS.getOrDefault(ct, "#38bdf8");
            traces.append(String.format("""
                {
                    "name": "%s",
                    "x": %s,
                    "y": %s,
                    "type": "bar",
                    "marker": { "color": "%s" },
                    "hovertemplate": "<b>%s</b> in %%{x}<br>Incidents: %%{y:,}<extra></extra>"
                }
            """, escapeJs(ct), xArr, yArr, color, escapeJs(ct)));
        }
        traces.append("]");

        String layoutJson = """
            {
                "template": "plotly_dark",
                "barmode": "stack",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 400,
                "margin": { "l": 50, "r": 30, "t": 60, "b": 100 },
                "title": {
                    "text": "<b>Environmental Vulnerability (Location vs Crime Category)</b>",
                    "x": 0.03, "xanchor": "left"
                },
                "xaxis": { "tickangle": -35, "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" },
                "yaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0", "title": "Crime Count" },
                "legend": { "bgcolor": "rgba(15,23,42,0.6)", "bordercolor": "rgba(255,255,255,0.1)", "borderwidth": 1 }
            }
        """;

        return wrapPlotly(divId, traces.toString(), layoutJson, 400);
    }

    public String plotVictimAgeDistribution(List<CrimeRecord> records) {
        String divId = "demographics_" + System.nanoTime();
        if (records == null || records.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No demographic data.</div>";
        }

        List<String> crimeTypes = records.stream().map(CrimeRecord::getCrimeType).distinct().sorted().toList();
        Map<String, List<Integer>> agesByCrime = new HashMap<>();
        for (CrimeRecord r : records) {
            agesByCrime.computeIfAbsent(r.getCrimeType(), k -> new ArrayList<>()).add(r.getVictimAge());
        }

        StringBuilder traces = new StringBuilder("[");
        for (int i = 0; i < crimeTypes.size(); i++) {
            String ct = crimeTypes.get(i);
            if (i > 0) traces.append(",");
            List<Integer> ages = agesByCrime.getOrDefault(ct, Collections.emptyList());
            String color = CRIME_TYPE_COLORS.getOrDefault(ct, "#38bdf8");

            traces.append(String.format("""
                {
                    "y": %s,
                    "name": "%s",
                    "type": "box",
                    "marker": { "color": "%s" },
                    "boxpoints": "outliers"
                }
            """, ages.toString(), escapeJs(ct), color));
        }
        traces.append("]");

        String layoutJson = """
            {
                "template": "plotly_dark",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 380,
                "margin": { "l": 50, "r": 30, "t": 60, "b": 50 },
                "title": {
                    "text": "<b>Victim Age Demographics Across Crime Categories</b>",
                    "x": 0.03, "xanchor": "left"
                },
                "xaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" },
                "yaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0", "title": "Victim Age (Years)" }
            }
        """;

        return wrapPlotly(divId, traces.toString(), layoutJson, 380);
    }

    public String plotFeatureImportanceBar(List<FeatureImportance> importances, int topN) {
        String divId = "feat_imp_" + System.nanoTime();
        if (importances == null || importances.isEmpty()) {
            return "<div style='color:#94a3b8; text-align:center; padding:30px;'>No feature importance data.</div>";
        }

        List<FeatureImportance> sub = new ArrayList<>(importances);
        if (sub.size() > topN) {
            sub = sub.subList(0, topN);
        }
        Collections.reverse(sub);

        StringBuilder xVals = new StringBuilder("[");
        StringBuilder yVals = new StringBuilder("[");
        for (int i = 0; i < sub.size(); i++) {
            if (i > 0) { xVals.append(","); yVals.append(","); }
            xVals.append(sub.get(i).getImportance());
            yVals.append("\"").append(escapeJs(sub.get(i).getFeature())).append("\"");
        }
        xVals.append("]");
        yVals.append("]");

        String dataJson = String.format("""
            [{
                "x": %s,
                "y": %s,
                "type": "bar",
                "orientation": "h",
                "marker": { "color": "#10b981", "line": { "color": "#0f172a", "width": 1 } },
                "hovertemplate": "<b>Feature:</b> %%{y}<br><b>Relative Weight:</b> %%{x:.4f}<extra></extra>"
            }]
        """, xVals, yVals);

        String layoutJson = """
            {
                "template": "plotly_dark",
                "paper_bgcolor": "rgba(0,0,0,0)",
                "plot_bgcolor": "rgba(15,23,42,0.4)",
                "font": { "color": "#e2e8f0", "family": "Inter, sans-serif" },
                "height": 340,
                "margin": { "l": 130, "r": 25, "t": 55, "b": 40 },
                "title": {
                    "text": "<b>Predictive Decision Drivers (Random Forest Weights)</b>",
                    "x": 0.03, "xanchor": "left"
                },
                "xaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0", "title": "Gini Importance" },
                "yaxis": { "gridcolor": "rgba(255,255,255,0.07)", "color": "#e2e8f0" }
            }
        """;

        return wrapPlotly(divId, dataJson, layoutJson, 340);
    }

    public String generateHotspotTableHtml(List<HotspotCluster> clusters) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class='data-table'><thead><tr>");
        sb.append("<th>Cluster ID</th><th>Total Incidents</th><th>% of All Crimes</th>");
        sb.append("<th>Primary Area</th><th>Dominant Crime Type</th><th>Primary Location</th><th>Centroid Coordinates</th>");
        sb.append("</tr></thead><tbody>");

        if (clusters != null && !clusters.isEmpty()) {
            for (HotspotCluster c : clusters) {
                sb.append(String.format("<tr><td><b>%s</b></td><td>%,d</td><td>%.2f%%</td><td>%s</td><td>%s</td><td>%s</td><td>%.5f, %.5f</td></tr>",
                        c.getClusterIdStr(), c.getTotalIncidents(), c.getPercentOfAllCrimes(),
                        escapeJs(c.getPrimaryArea()), escapeJs(c.getDominantCrimeType()),
                        escapeJs(c.getPrimaryLocation()), c.getCentroidLatitude(), c.getCentroidLongitude()));
            }
        } else {
            sb.append("<tr><td colspan='7' style='text-align:center;'>No core clusters detected with current thresholds.</td></tr>");
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }

    public String generateAreaRankingsTableHtml(List<AreaRanking> rankings) {
        StringBuilder sb = new StringBuilder();
        sb.append("<table class='data-table'><thead><tr>");
        sb.append("<th>Rank</th><th>Area / Neighborhood</th><th>Total Crimes</th><th>% Share</th><th>Primary Crime Type</th><th>Mean Victim Age</th>");
        sb.append("</tr></thead><tbody>");

        if (rankings != null && !rankings.isEmpty()) {
            for (AreaRanking r : rankings) {
                sb.append(String.format("<tr><td>#%d</td><td><b>%s</b></td><td>%,d</td><td>%.2f%%</td><td>%s</td><td>%.1f yrs</td></tr>",
                        r.getRank(), escapeJs(r.getArea()), r.getTotalCrimes(), r.getPercentShare(),
                        escapeJs(r.getTopCrimeType()), r.getAvgVictimAge()));
            }
        }
        sb.append("</tbody></table>");
        return sb.toString();
    }
}

package com.crimescope.controller;

import com.crimescope.model.AreaRanking;
import com.crimescope.model.CrimeRecord;
import com.crimescope.model.FeatureImportance;
import com.crimescope.model.HotspotCluster;
import com.crimescope.service.AnalyticsService;
import com.crimescope.service.CrimeDataService;
import com.crimescope.service.HotspotService;
import com.crimescope.service.RiskForecastingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.*;
import java.util.stream.Collectors;

@Controller
public class DashboardController {

    private final CrimeDataService dataService;
    private final HotspotService hotspotService;
    private final RiskForecastingService riskService;
    private final AnalyticsService analyticsService;

    public DashboardController(CrimeDataService dataService,
                               HotspotService hotspotService,
                               RiskForecastingService riskService,
                               AnalyticsService analyticsService) {
        this.dataService = dataService;
        this.hotspotService = hotspotService;
        this.riskService = riskService;
        this.analyticsService = analyticsService;
    }

    @GetMapping("/")
    public String index(Model model) {
        List<CrimeRecord> df = dataService.getAllCleanRecords();

        int totalRecords = df.size();
        String mostCommonCrime = "N/A";
        long mostCommonCount = 0;

        if (!df.isEmpty()) {
            Map<String, Long> crimeCounts = df.stream()
                    .collect(Collectors.groupingBy(CrimeRecord::getCrimeType, Collectors.counting()));
            Map.Entry<String, Long> topCrime = crimeCounts.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElse(null);
            if (topCrime != null) {
                mostCommonCrime = topCrime.getKey();
                mostCommonCount = topCrime.getValue();
            }
        }

        // Hotspot Analysis
        HotspotService.HotspotResult hotspotResult = hotspotService.runDbscanHotspots(df, 0.60, 25);
        List<HotspotCluster> clusterSummaries = hotspotResult.getClusterSummaries();
        List<AreaRanking> areaRankings = hotspotResult.getAreaRankings();

        String highestRiskArea = areaRankings.isEmpty() ? "N/A" : areaRankings.get(0).getArea();

        // Tables
        String hotspotTableHtml = analyticsService.generateHotspotTableHtml(clusterSummaries);
        String areaRankingsTableHtml = analyticsService.generateAreaRankingsTableHtml(areaRankings);

        // Date bounds
        String minDate = dataService.getMinDate();
        String maxDate = dataService.getMaxDate();
        String defaultDate = maxDate;

        long defaultDayCount = df.stream().filter(r -> defaultDate.equals(r.getDate())).count();

        // Charts
        String chartCategoryHtml = analyticsService.plotCrimeCategoryDistribution(df);
        String chartTimelineHtml = analyticsService.plotCrimeTrendsTimeline(df, df);
        String chartHourlyHtml = analyticsService.plotHourlyDistribution(df);
        String chartAreasHtml = analyticsService.plotTopAreasBar(df, 10);
        String chartMapHtml = analyticsService.plotHotspotMap(hotspotResult.getClusteredRecords(), clusterSummaries, "crime_type");
        String chartHeatmapHtml = analyticsService.plotDayHourHeatmap(df);
        String chartCrosstabHtml = analyticsService.plotLocationCrimeCrosstab(df);
        String chartDemographicsHtml = analyticsService.plotVictimAgeDistribution(df);

        List<FeatureImportance> fiList = riskService.getMetrics() != null ?
                riskService.getMetrics().getFeatureImportance() : Collections.emptyList();
        String defaultFeatureChart = analyticsService.plotFeatureImportanceBar(fiList, 8);

        // Dropdowns
        List<String> allAreas = df.stream().map(CrimeRecord::getArea).distinct().sorted().toList();
        List<String> allCrimeTypes = df.stream().map(CrimeRecord::getCrimeType).distinct().sorted().toList();
        List<String> allLocations = df.stream().map(CrimeRecord::getLocationType).distinct().sorted().toList();
        List<Integer> allYears = df.stream().map(CrimeRecord::getYear).distinct().sorted().toList();

        List<Map<String, Object>> monthsList = List.of(
                Map.of("id", 1, "name", "January"), Map.of("id", 2, "name", "February"),
                Map.of("id", 3, "name", "March"), Map.of("id", 4, "name", "April"),
                Map.of("id", 5, "name", "May"), Map.of("id", 6, "name", "June"),
                Map.of("id", 7, "name", "July"), Map.of("id", 8, "name", "August"),
                Map.of("id", 9, "name", "September"), Map.of("id", 10, "name", "October"),
                Map.of("id", 11, "name", "November"), Map.of("id", 12, "name", "December")
        );

        // Dataset sample (first 100 rows)
        List<CrimeRecord> datasetSample = df.stream().limit(100).toList();

        // Demographics
        double avgVictimAge = Math.round(df.stream().mapToInt(CrimeRecord::getVictimAge).average().orElse(35.0) * 10.0) / 10.0;
        int minVictimAge = df.stream().mapToInt(CrimeRecord::getVictimAge).min().orElse(18);
        int maxVictimAge = df.stream().mapToInt(CrimeRecord::getVictimAge).max().orElse(75);
        int medianVictimAge = (int) avgVictimAge;

        model.addAttribute("total_records", totalRecords);
        model.addAttribute("num_hotspots", hotspotResult.getStats().getNumClusters());
        model.addAttribute("highest_risk_area", highestRiskArea);
        model.addAttribute("most_common_crime", mostCommonCrime);
        model.addAttribute("most_common_count", mostCommonCount);
        model.addAttribute("dbscan_stats", hotspotResult.getStats());

        model.addAttribute("all_areas", allAreas);
        model.addAttribute("all_crime_types", allCrimeTypes);
        model.addAttribute("all_locations", allLocations);
        model.addAttribute("all_years", allYears);
        model.addAttribute("months_list", monthsList);

        model.addAttribute("min_date", minDate);
        model.addAttribute("max_date", maxDate);
        model.addAttribute("default_date", defaultDate);
        model.addAttribute("default_day_count", defaultDayCount);

        model.addAttribute("chart_category_html", chartCategoryHtml);
        model.addAttribute("chart_timeline_html", chartTimelineHtml);
        model.addAttribute("chart_hourly_html", chartHourlyHtml);
        model.addAttribute("chart_areas_html", chartAreasHtml);
        model.addAttribute("chart_map_html", chartMapHtml);
        model.addAttribute("chart_heatmap_html", chartHeatmapHtml);
        model.addAttribute("chart_crosstab_html", chartCrosstabHtml);
        model.addAttribute("chart_demographics_html", chartDemographicsHtml);
        model.addAttribute("default_feature_chart", defaultFeatureChart);

        model.addAttribute("hotspot_table_html", hotspotTableHtml);
        model.addAttribute("area_rankings_table_html", areaRankingsTableHtml);
        model.addAttribute("dataset_sample", datasetSample);

        model.addAttribute("avg_victim_age", avgVictimAge);
        model.addAttribute("median_victim_age", medianVictimAge);
        model.addAttribute("min_victim_age", minVictimAge);
        model.addAttribute("max_victim_age", maxVictimAge);

        return "index";
    }
}

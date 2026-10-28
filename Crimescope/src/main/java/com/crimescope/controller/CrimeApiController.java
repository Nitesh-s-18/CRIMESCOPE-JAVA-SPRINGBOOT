package com.crimescope.controller;

import com.crimescope.model.*;
import com.crimescope.service.AnalyticsService;
import com.crimescope.service.CrimeDataService;
import com.crimescope.service.HotspotService;
import com.crimescope.service.RiskForecastingService;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class CrimeApiController {

    private final CrimeDataService dataService;
    private final HotspotService hotspotService;
    private final RiskForecastingService riskService;
    private final AnalyticsService analyticsService;

    public CrimeApiController(CrimeDataService dataService,
                              HotspotService hotspotService,
                              RiskForecastingService riskService,
                              AnalyticsService analyticsService) {
        this.dataService = dataService;
        this.hotspotService = hotspotService;
        this.riskService = riskService;
        this.analyticsService = analyticsService;
    }

    @PostMapping("/filter")
    public ResponseEntity<FilterResponse> filterData(@RequestBody FilterRequest req) {
        List<CrimeRecord> filtered = dataService.filterRecords(req);
        int total = filtered.size();

        String mostCommonCrime = "N/A";
        int mostCommonCount = 0;
        if (!filtered.isEmpty()) {
            Map<String, Long> counts = filtered.stream()
                    .collect(Collectors.groupingBy(CrimeRecord::getCrimeType, Collectors.counting()));
            Map.Entry<String, Long> top = counts.entrySet().stream()
                    .max(Map.Entry.comparingByValue()).orElse(null);
            if (top != null) {
                mostCommonCrime = top.getKey();
                mostCommonCount = top.getValue().intValue();
            }
        }

        HotspotService.HotspotResult hr = hotspotService.runDbscanHotspots(filtered, 0.60, 25);
        String highestRiskArea = hr.getAreaRankings().isEmpty() ? "N/A" : hr.getAreaRankings().get(0).getArea();

        FilterResponse resp = new FilterResponse();
        resp.setTotalRecords(total);
        resp.setNumHotspots(hr.getStats().getNumClusters());
        resp.setHighestRiskArea(highestRiskArea);
        resp.setMostCommonCrime(mostCommonCrime);
        resp.setMostCommonCount(mostCommonCount);

        // Compute descriptive active label
        String label;
        if ("single_day".equalsIgnoreCase(req.getMode()) && req.getExactDate() != null && !req.getExactDate().isEmpty()) {
            label = "Showing: Single Day (" + req.getExactDate() + ") (" + String.format("%,d", total) + " Incidents)";
        } else if ("year_month".equalsIgnoreCase(req.getMode())) {
            label = "Showing: Year " + req.getYear() + ", Month " + req.getMonth() + " (" + String.format("%,d", total) + " Incidents)";
        } else if ("range".equalsIgnoreCase(req.getMode())) {
            label = "Showing: Range (" + req.getStartDate() + " to " + req.getEndDate() + ") (" + String.format("%,d", total) + " Incidents)";
        } else {
            label = "Showing: All Records (2023-2024) (" + String.format("%,d", total) + " Incidents)";
        }
        resp.setActiveLabel(label);

        // Generate Plotly charts
        resp.setChartCategory(analyticsService.plotCrimeCategoryDistribution(filtered));
        resp.setChartTimeline(analyticsService.plotCrimeTrendsTimeline(filtered, dataService.getAllCleanRecords()));
        resp.setChartHourly(analyticsService.plotHourlyDistribution(filtered));
        resp.setChartAreas(analyticsService.plotTopAreasBar(filtered, 10));
        resp.setChartMap(analyticsService.plotHotspotMap(hr.getClusteredRecords(), hr.getClusterSummaries(), "crime_type"));

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/hotspots")
    public ResponseEntity<HotspotResponse> getHotspots(@RequestBody HotspotRequest req) {
        double eps = req.getEpsKm() > 0 ? req.getEpsKm() : 0.60;
        int minPts = req.getMinSamples() > 0 ? req.getMinSamples() : 25;
        String colorBy = (req.getColorBy() != null && !req.getColorBy().isEmpty()) ? req.getColorBy() : "crime_type";

        List<CrimeRecord> records = dataService.getAllCleanRecords();
        HotspotService.HotspotResult hr = hotspotService.runDbscanHotspots(records, eps, minPts);

        String mapHtml = analyticsService.plotHotspotMap(hr.getClusteredRecords(), hr.getClusterSummaries(), colorBy);
        String tableHtml = analyticsService.generateHotspotTableHtml(hr.getClusterSummaries());

        HotspotResponse resp = new HotspotResponse(hr.getStats(), mapHtml, tableHtml);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/predict")
    public ResponseEntity<PredictionResponse> predict(@RequestBody PredictionRequest req) {
        PredictionResponse resp = riskService.predictRisk(
                req.getArea(),
                req.getDayOfWeek(),
                req.getHour(),
                req.getMonth(),
                req.getLocationType()
        );

        List<FeatureImportance> fiList = riskService.getMetrics() != null ?
                riskService.getMetrics().getFeatureImportance() : Collections.emptyList();
        String featureChartHtml = analyticsService.plotFeatureImportanceBar(fiList, 8);
        resp.setFeatureChart(featureChartHtml);

        return ResponseEntity.ok(resp);
    }

    @GetMapping("/download_csv")
    public ResponseEntity<Resource> downloadCsv() {
        File file = new File("data/crime_data.csv");
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"crime_data.csv\"")
                .contentType(MediaType.parseMediaType("text/csv"))
                .body(resource);
    }

    @PostMapping("/regenerate")
    public ResponseEntity<Map<String, String>> regenerateDataset() {
        dataService.regenerateDataset();
        return ResponseEntity.ok(Map.of(
                "status", "success",
                "message", "Dataset successfully regenerated and SQLite database reset."
        ));
    }
}

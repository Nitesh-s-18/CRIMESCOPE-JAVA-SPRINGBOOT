package com.crimescope.service;

import com.crimescope.model.CrimeRecord;
import com.crimescope.model.FilterRequest;
import com.crimescope.repository.CrimeRecordRepository;
import jakarta.annotation.PostConstruct;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVPrinter;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

@Service
public class CrimeDataService {

    private static final Logger log = LoggerFactory.getLogger(CrimeDataService.class);

    private final CrimeRecordRepository repository;
    private final PreprocessingService preprocessingService;
    private final RiskForecastingService riskForecastingService;

    @Value("${crimescope.csv-path:data/crime_data.csv}")
    private String csvPath;

    @Value("${crimescope.db-path:data/crimescope.db}")
    private String dbPath;

    private List<CrimeRecord> allCleanRecords = new CopyOnWriteArrayList<>();
    private String minDate = "2023-01-01";
    private String maxDate = "2024-12-30";

    public CrimeDataService(CrimeRecordRepository repository,
                            PreprocessingService preprocessingService,
                            RiskForecastingService riskForecastingService) {
        this.repository = repository;
        this.preprocessingService = preprocessingService;
        this.riskForecastingService = riskForecastingService;
    }

    @PostConstruct
    public void initialize() {
        log.info("Initializing CrimeScope Data Science & Persistence Engine...");
        repository.initSchema();

        int dbCount = repository.count();
        log.info("Current records in SQLite database: {}", dbCount);

        List<CrimeRecord> rawRecords;
        File csvFile = new File(csvPath);

        if (dbCount < 5000) {
            if (csvFile.exists() && csvFile.length() > 500) {
                log.info("Ingesting records from CSV file: {}", csvPath);
                rawRecords = readCsv(csvFile);
            } else {
                log.info("Generating 6,000 synthetic incident records...");
                rawRecords = generateSyntheticData(6000, 42);
                writeCsv(rawRecords, csvFile);
            }
            repository.saveAll(rawRecords);
        } else {
            log.info("Loading records directly from SQLite database...");
            rawRecords = repository.findAll();
        }

        // Preprocess records
        List<CrimeRecord> cleaned = preprocessingService.cleanAndPreprocess(rawRecords);
        this.allCleanRecords = new CopyOnWriteArrayList<>(cleaned);

        if (!allCleanRecords.isEmpty()) {
            this.minDate = allCleanRecords.stream().map(CrimeRecord::getDate).min(String::compareTo).orElse("2023-01-01");
            this.maxDate = allCleanRecords.stream().map(CrimeRecord::getDate).max(String::compareTo).orElse("2024-12-30");
        }

        log.info("Successfully preprocessed {} crime records. Date bounds: {} to {}",
                allCleanRecords.size(), minDate, maxDate);

        // Train Risk Forecasting Model
        log.info("Training supervised Random Forest risk forecasting model...");
        riskForecastingService.trainAndEvaluate(allCleanRecords);
        log.info("Risk forecasting model ready! Accuracy: {}%",
                Math.round(riskForecastingService.getMetrics().getAccuracy() * 10000.0) / 100.0);
    }

    public List<CrimeRecord> getAllCleanRecords() {
        return allCleanRecords;
    }

    public String getMinDate() { return minDate; }
    public String getMaxDate() { return maxDate; }

    public List<CrimeRecord> filterRecords(FilterRequest req) {
        if (req == null) return new ArrayList<>(allCleanRecords);

        String exactDate = req.getExactDate() != null ? req.getExactDate().trim() : "";
        String startDate = req.getStartDate() != null ? req.getStartDate().trim() : "";
        String endDate = req.getEndDate() != null ? req.getEndDate().trim() : "";
        String year = req.getYear() != null ? req.getYear().trim() : "ALL";
        String month = req.getMonth() != null ? req.getMonth().trim() : "ALL";
        String area = req.getArea() != null ? req.getArea().trim() : "ALL";
        String crimeType = req.getCrimeType() != null ? req.getCrimeType().trim() : "ALL";

        return allCleanRecords.stream().filter(r -> {
            // Priority 1: Exact Date
            if (!exactDate.isEmpty()) {
                if (!r.getDate().equals(exactDate)) return false;
            }
            // Priority 2: Year and/or Month
            else if ((!year.equals("ALL") && year.matches("\\d+")) || (!month.equals("ALL") && month.matches("\\d+"))) {
                if (!year.equals("ALL") && year.matches("\\d+")) {
                    int y = Integer.parseInt(year);
                    if (r.getYear() != y) return false;
                }
                if (!month.equals("ALL") && month.matches("\\d+")) {
                    int m = Integer.parseInt(month);
                    if (r.getMonth() != m) return false;
                }
            }
            // Priority 3: Custom Date Range
            else if (!startDate.isEmpty() && !endDate.isEmpty()) {
                if (r.getDate().compareTo(startDate) < 0 || r.getDate().compareTo(endDate) > 0) {
                    return false;
                }
            }

            // Filter Area
            if (!area.equals("ALL") && !r.getArea().equalsIgnoreCase(area)) {
                return false;
            }

            // Filter Crime Category
            if (!crimeType.equals("ALL") && !r.getCrimeType().equalsIgnoreCase(crimeType)) {
                return false;
            }

            return true;
        }).collect(Collectors.toList());
    }

    public synchronized void regenerateDataset() {
        log.info("Regenerating dataset with new random seed...");
        long newSeed = System.currentTimeMillis() % 10000;
        List<CrimeRecord> newRecords = generateSyntheticData(6000, newSeed);

        // Write to CSV
        File csvFile = new File(csvPath);
        writeCsv(newRecords, csvFile);

        // Reset and insert in SQLite
        repository.deleteAll();
        repository.saveAll(newRecords);

        // Reprocess
        List<CrimeRecord> cleaned = preprocessingService.cleanAndPreprocess(newRecords);
        this.allCleanRecords = new CopyOnWriteArrayList<>(cleaned);

        if (!allCleanRecords.isEmpty()) {
            this.minDate = allCleanRecords.stream().map(CrimeRecord::getDate).min(String::compareTo).orElse("2023-01-01");
            this.maxDate = allCleanRecords.stream().map(CrimeRecord::getDate).max(String::compareTo).orElse("2024-12-30");
        }

        riskForecastingService.trainAndEvaluate(allCleanRecords);
        log.info("Dataset regenerated and model retrained successfully.");
    }

    private List<CrimeRecord> readCsv(File file) {
        List<CrimeRecord> list = new ArrayList<>();
        try (Reader reader = new InputStreamReader(new FileInputStream(file), StandardCharsets.UTF_8);
             CSVParser parser = new CSVParser(reader, CSVFormat.DEFAULT.builder().setHeader().setSkipHeaderRecord(true).setIgnoreHeaderCase(true).setTrim(true).build())) {

            for (CSVRecord rec : parser) {
                CrimeRecord r = new CrimeRecord();
                r.setCrimeId(rec.get("crime_id"));
                r.setDate(rec.get("date"));
                r.setTime(rec.get("time"));
                r.setCity(rec.isSet("city") ? rec.get("city") : "Coimbatore");
                r.setArea(rec.get("area"));
                r.setLatitude(Double.parseDouble(rec.get("latitude")));
                r.setLongitude(Double.parseDouble(rec.get("longitude")));
                r.setCrimeType(rec.get("crime_type"));
                r.setVictimAge(Integer.parseInt(rec.get("victim_age")));
                r.setLocationType(rec.get("location_type"));
                r.setDayOfWeek(rec.isSet("day_of_week") ? rec.get("day_of_week") : "");
                r.setHour(Integer.parseInt(rec.get("hour")));
                r.setMonth(Integer.parseInt(rec.get("month")));
                list.add(r);
            }
        } catch (Exception e) {
            log.error("Failed to read CSV: {}", e.getMessage(), e);
        }
        return list;
    }

    private void writeCsv(List<CrimeRecord> records, File file) {
        if (file.getParentFile() != null) file.getParentFile().mkdirs();
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8);
             CSVPrinter printer = new CSVPrinter(writer, CSVFormat.DEFAULT.builder().setHeader(
                     "crime_id", "date", "time", "city", "area", "latitude", "longitude",
                     "crime_type", "victim_age", "location_type", "day_of_week", "hour", "month"
             ).build())) {

            for (CrimeRecord r : records) {
                printer.printRecord(
                        r.getCrimeId(), r.getDate(), r.getTime(), r.getCity(), r.getArea(),
                        r.getLatitude(), r.getLongitude(), r.getCrimeType(), r.getVictimAge(),
                        r.getLocationType(), r.getDayOfWeek(), r.getHour(), r.getMonth()
                );
            }
            printer.flush();
        } catch (Exception e) {
            log.error("Failed to write CSV: {}", e.getMessage(), e);
        }
    }

    private List<CrimeRecord> generateSyntheticData(int count, long seed) {
        Random rng = new Random(seed);
        List<CrimeRecord> list = new ArrayList<>();

        Map<String, double[]> areaCoords = new LinkedHashMap<>();
        areaCoords.put("Gandhipuram", new double[]{11.0180, 76.9676});
        areaCoords.put("RS Puram", new double[]{11.0125, 76.9450});
        areaCoords.put("Peelamedu", new double[]{11.0310, 77.0020});
        areaCoords.put("Saibaba Colony", new double[]{11.0280, 76.9420});
        areaCoords.put("Ukkadam", new double[]{10.9900, 76.9600});
        areaCoords.put("Singanallur", new double[]{10.9990, 77.0220});
        areaCoords.put("Town Hall", new double[]{10.9960, 76.9630});
        areaCoords.put("Ganapathy", new double[]{11.0420, 76.9780});
        areaCoords.put("Saravanampatti", new double[]{11.0790, 77.0010});
        areaCoords.put("Ramanathapuram", new double[]{10.9950, 76.9850});
        areaCoords.put("Kuniyamuthur", new double[]{10.9570, 76.9450});
        areaCoords.put("Vadavalli", new double[]{11.0180, 76.9020});
        areaCoords.put("Thudiyalur", new double[]{11.0770, 76.9410});
        areaCoords.put("Kovaipudur", new double[]{10.9380, 76.9240});

        List<String> areas = new ArrayList<>(areaCoords.keySet());
        String[] crimeTypes = {"Theft", "Vehicle Theft", "Burglary", "Fraud", "Robbery", "Vandalism", "Assault"};
        String[] locationTypes = {
                "Commercial Area", "Residential Area", "Street / Highway",
                "Market / Bazaar", "Public Transport / Transit", "Educational Campus",
                "ATM / Bank", "Parking Lot"
        };

        LocalDate startDate = LocalDate.of(2023, 1, 1);
        int totalDays = 730;

        for (int i = 1; i <= count; i++) {
            String area = areas.get(rng.nextInt(areas.size()));
            double[] center = areaCoords.get(area);
            double lat = center[0] + (rng.nextGaussian() * 0.0055);
            double lon = center[1] + (rng.nextGaussian() * 0.0055);

            LocalDate date = startDate.plusDays(rng.nextInt(totalDays));
            int hour = (rng.nextDouble() < 0.65) ? (16 + rng.nextInt(7)) : rng.nextInt(24);
            int minute = rng.nextInt(60);
            int second = rng.nextInt(60);
            String timeStr = String.format("%02d:%02d:%02d", hour, minute, second);

            String crimeType = crimeTypes[rng.nextInt(crimeTypes.length)];
            String locType = locationTypes[rng.nextInt(locationTypes.length)];
            int victimAge = 18 + rng.nextInt(58);

            String id = String.format("CS-%06d", i);
            CrimeRecord r = new CrimeRecord(
                    id, date.toString(), timeStr, "Coimbatore", area,
                    Math.round(lat * 10000.0) / 10000.0,
                    Math.round(lon * 10000.0) / 10000.0,
                    crimeType, victimAge, locType,
                    date.getDayOfWeek().name(), hour, date.getMonthValue()
            );
            list.add(r);
        }

        return list;
    }
}

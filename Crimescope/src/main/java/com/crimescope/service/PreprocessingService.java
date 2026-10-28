package com.crimescope.service;

import com.crimescope.model.CrimeRecord;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
public class PreprocessingService {

    private static final DateTimeFormatter[] DATE_FORMATTERS = new DateTimeFormatter[]{
        DateTimeFormatter.ofPattern("yyyy-MM-dd"),
        DateTimeFormatter.ofPattern("yyyy/MM/dd"),
        DateTimeFormatter.ofPattern("dd-MM-yyyy")
    };

    public static String getTimeSlot(int hour) {
        if (6 <= hour && hour < 12) {
            return "Morning (06:00-12:00)";
        } else if (12 <= hour && hour < 17) {
            return "Afternoon (12:00-17:00)";
        } else if (17 <= hour && hour < 22) {
            return "Evening (17:00-22:00)";
        } else {
            return "Night (22:00-06:00)";
        }
    }

    public static String getSeason(int month) {
        if (month == 12 || month == 1 || month == 2) {
            return "Winter";
        } else if (month >= 3 && month <= 5) {
            return "Summer";
        } else if (month >= 6 && month <= 9) {
            return "Southwest Monsoon";
        } else {
            return "Northeast Monsoon";
        }
    }

    public List<CrimeRecord> cleanAndPreprocess(List<CrimeRecord> records) {
        List<CrimeRecord> cleaned = new ArrayList<>();
        if (records == null) return cleaned;

        for (CrimeRecord r : records) {
            // Geographic sanity check for Coimbatore region
            if (r.getLatitude() < 10.5 || r.getLatitude() > 11.5 ||
                r.getLongitude() < 76.5 || r.getLongitude() > 77.5) {
                continue;
            }

            // Parse Date
            LocalDate localDate = null;
            for (DateTimeFormatter fmt : DATE_FORMATTERS) {
                try {
                    localDate = LocalDate.parse(r.getDate().trim(), fmt);
                    break;
                } catch (Exception ignored) {}
            }
            if (localDate == null) {
                try {
                    localDate = LocalDate.parse(r.getDate().trim().substring(0, 10));
                } catch (Exception ignored) {
                    continue;
                }
            }

            int hour = r.getHour();
            int minute = 0;
            int second = 0;
            if (r.getTime() != null && r.getTime().contains(":")) {
                try {
                    String[] parts = r.getTime().trim().split(":");
                    hour = Integer.parseInt(parts[0]);
                    if (parts.length > 1) minute = Integer.parseInt(parts[1]);
                    if (parts.length > 2) second = Integer.parseInt(parts[2].split("\\.")[0]);
                } catch (Exception ignored) {}
            }

            LocalDateTime dt = LocalDateTime.of(localDate.getYear(), localDate.getMonthValue(), localDate.getDayOfMonth(), hour, minute, second);
            r.setDatetime(dt);
            r.setHour(hour);
            r.setMonth(localDate.getMonthValue());
            r.setDayOfMonth(localDate.getDayOfMonth());
            r.setQuarter((localDate.getMonthValue() - 1) / 3 + 1);
            r.setYear(localDate.getYear());

            String dayName = localDate.getDayOfWeek().name().substring(0, 1) +
                localDate.getDayOfWeek().name().substring(1).toLowerCase();
            r.setDayOfWeek(dayName);

            boolean weekend = "Saturday".equalsIgnoreCase(dayName) || "Sunday".equalsIgnoreCase(dayName);
            r.setIsWeekend(weekend ? 1 : 0);

            r.setTimeSlot(getTimeSlot(hour));
            r.setSeason(getSeason(r.getMonth()));

            // Cyclical Trigonometric Features
            r.setHourSin(Math.sin(2 * Math.PI * hour / 24.0));
            r.setHourCos(Math.cos(2 * Math.PI * hour / 24.0));

            r.setMonthSin(Math.sin(2 * Math.PI * r.getMonth() / 12.0));
            r.setMonthCos(Math.cos(2 * Math.PI * r.getMonth() / 12.0));

            int dayOfWeekNum = localDate.getDayOfWeek().getValue() - 1; // 0 = Mon, 6 = Sun
            r.setDaySin(Math.sin(2 * Math.PI * dayOfWeekNum / 7.0));
            r.setDayCos(Math.cos(2 * Math.PI * dayOfWeekNum / 7.0));

            cleaned.add(r);
        }

        return cleaned;
    }
}

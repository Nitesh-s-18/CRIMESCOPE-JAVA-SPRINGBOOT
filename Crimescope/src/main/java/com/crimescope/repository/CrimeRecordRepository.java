package com.crimescope.repository;

import com.crimescope.model.CrimeRecord;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

@Repository
public class CrimeRecordRepository {

    private final JdbcTemplate jdbcTemplate;

    public CrimeRecordRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public void initSchema() {
        String createTableSql = """
            CREATE TABLE IF NOT EXISTS crimes (
                crime_id TEXT PRIMARY KEY,
                date TEXT NOT NULL,
                time TEXT NOT NULL,
                city TEXT NOT NULL,
                area TEXT NOT NULL,
                latitude REAL NOT NULL,
                longitude REAL NOT NULL,
                crime_type TEXT NOT NULL,
                victim_age INTEGER NOT NULL,
                location_type TEXT NOT NULL,
                day_of_week TEXT NOT NULL,
                hour INTEGER NOT NULL,
                month INTEGER NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            );
        """;
        jdbcTemplate.execute(createTableSql);
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_crimes_area ON crimes(area);");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_crimes_date ON crimes(date);");
        jdbcTemplate.execute("CREATE INDEX IF NOT EXISTS idx_crimes_crime_type ON crimes(crime_type);");
    }

    public int count() {
        Integer cnt = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM crimes", Integer.class);
        return cnt != null ? cnt : 0;
    }

    public List<CrimeRecord> findAll() {
        String sql = "SELECT crime_id, date, time, city, area, latitude, longitude, crime_type, victim_age, location_type, day_of_week, hour, month FROM crimes";
        return jdbcTemplate.query(sql, new CrimeRecordRowMapper());
    }

    public void saveAll(List<CrimeRecord> records) {
        String sql = """
            INSERT OR IGNORE INTO crimes (
                crime_id, date, time, city, area, latitude, longitude,
                crime_type, victim_age, location_type, day_of_week, hour, month
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;

        List<Object[]> batchArgs = new ArrayList<>();
        for (CrimeRecord r : records) {
            batchArgs.add(new Object[]{
                r.getCrimeId(), r.getDate(), r.getTime(), r.getCity(), r.getArea(),
                r.getLatitude(), r.getLongitude(), r.getCrimeType(), r.getVictimAge(),
                r.getLocationType(), r.getDayOfWeek(), r.getHour(), r.getMonth()
            });
        }
        jdbcTemplate.batchUpdate(sql, batchArgs);
    }

    public void deleteAll() {
        jdbcTemplate.execute("DELETE FROM crimes");
    }

    private static class CrimeRecordRowMapper implements RowMapper<CrimeRecord> {
        @Override
        public CrimeRecord mapRow(ResultSet rs, int rowNum) throws SQLException {
            CrimeRecord r = new CrimeRecord();
            r.setCrimeId(rs.getString("crime_id"));
            r.setDate(rs.getString("date"));
            r.setTime(rs.getString("time"));
            r.setCity(rs.getString("city"));
            r.setArea(rs.getString("area"));
            r.setLatitude(rs.getDouble("latitude"));
            r.setLongitude(rs.getDouble("longitude"));
            r.setCrimeType(rs.getString("crime_type"));
            r.setVictimAge(rs.getInt("victim_age"));
            r.setLocationType(rs.getString("location_type"));
            r.setDayOfWeek(rs.getString("day_of_week"));
            r.setHour(rs.getInt("hour"));
            r.setMonth(rs.getInt("month"));
            return r;
        }
    }
}

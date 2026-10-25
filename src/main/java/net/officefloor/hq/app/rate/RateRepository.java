package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * The rates for each grade on each sheet, each in force from its from-date (null: from the beginning
 * of time). A grade without one in force on a date has no rate on that date.
 */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, n) -> {
        Date from = rs.getDate("from_date");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate"),
                from == null ? null : from.toLocalDate());
    };

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade, then by from-date (none first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, rate, from_date FROM rate WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /**
     * The grade's rate in force on the date: the one with the latest from-date on or before it, a rate
     * without a from-date counting as the earliest. Null when none applies.
     */
    public BigDecimal find(String sheetId, String grade, LocalDate on) {
        return jdbc.query("SELECT sheet_id, grade, rate, from_date FROM rate WHERE sheet_id = ? AND grade = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1", ROW,
                sheetId, grade, Date.valueOf(on)).stream().findFirst().map(Rate::rate).orElse(null);
    }

    /** Set a grade's rate from a date (null: from the beginning of time), replacing one already there. */
    public Rate set(String sheetId, String grade, BigDecimal rate, LocalDate from) {
        Date fromDate = from == null ? null : Date.valueOf(from);
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                sheetId, grade, fromDate);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date) VALUES (?, ?, ?, ?)", sheetId, grade, rate,
                fromDate);
        return new Rate(sheetId, grade, rate, from);
    }
}

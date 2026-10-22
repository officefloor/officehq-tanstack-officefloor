package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Rates by sheet: per grade, at most one per from-date (and at most one with no from-date). */
@Repository
public class RateRepository implements Rates {

    private static final RowMapper<Rate> ROW = (rs, i) -> new Rate(rs.getString("sheet_id"),
            rs.getString("grade"), rs.getBigDecimal("pct"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class));

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates by grade, each grade's earliest first (the one with no from-date before all). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT * FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /** The grade's rate in force on the date: the one whose from-date is the latest on or before it (no
     *  from-date counting as the beginning of time); empty when none applies, or that one's to-date is before the date. */
    @Override
    public Optional<Rate> on(String sheetId, String grade, LocalDate date) {
        return Rates.on(list(sheetId), grade, date);
    }

    /** The sheet's rates as they stood at the end of the day: each set from a from-date replaces the one set before
     *  it for the grade from that same date. */
    public Rates asOf(String sheetId, LocalDate end) {
        Map<String, Rate> byGradeFrom = new LinkedHashMap<>();
        jdbc.query("SELECT * FROM rate_log WHERE sheet_id = ? AND (made_on IS NULL OR made_on <= ?) ORDER BY id",
                rs -> {
                    Rate r = ROW.mapRow(rs, 0);
                    byGradeFrom.put(r.grade() + "|" + r.from(), r);
                }, sheetId, end);
        List<Rate> rates = List.copyOf(byGradeFrom.values());
        return (sheet, grade, date) -> Rates.on(rates, grade, date);
    }

    /** Set the grade's rate from the from-date (null: from the beginning of time) through the to-date (null:
     *  with no end), replacing any it had from that same date. */
    public Rate set(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
        Date fromDate = from != null ? Date.valueOf(from) : null;
        Date toDate = to != null ? Date.valueOf(to) : null;
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                sheetId, grade, fromDate);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                sheetId, grade, pct, fromDate, toDate);
        jdbc.update("INSERT INTO rate_log (sheet_id, grade, pct, from_date, to_date, made_on) VALUES (?, ?, ?, ?, ?, "
                + "(SELECT today FROM sheet WHERE id = ?))", sheetId, grade, pct, fromDate, toDate, sheetId);
        return new Rate(sheetId, grade, pct, from, to);
    }
}

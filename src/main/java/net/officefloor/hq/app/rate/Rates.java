package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The store of rates, one per grade and from-date, kept per sheet. */
@Repository
public class Rates {

    private static final RowMapper<Rate> ROW = (rs, n) -> {
        Date from = rs.getDate("from_date");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate"),
                from == null ? null : from.toLocalDate());
    };

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade, then by from-date (one with none first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT * FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /** Sets the rate, replacing any the grade already has for the same from-date. */
    public void save(Rate rate) {
        Date from = rate.from() == null ? null : Date.valueOf(rate.from());
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                rate.sheetId(), rate.grade(), from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date) VALUES (?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate(), from);
    }

    /** The grade's rate on the date: the one with the latest from-date on or before it; null when none applies. */
    public BigDecimal on(String sheetId, String grade, LocalDate date) {
        return list(sheetId).stream()
                .filter(r -> r.grade().equals(grade) && (r.from() == null || !r.from().isAfter(date)))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Rate::rate).orElse(null);
    }
}

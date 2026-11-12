package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rates for each grade, kept per sheet, each applying from its from-date. A rate with no
 *  from-date applies from the beginning of time (stored as {@link #START}). A grade with no rate is
 *  absent. */
@Repository
public class RateStore {

    /** The stored from-date of a rate that applies from the beginning of time. */
    private static final LocalDate START = LocalDate.of(1, 1, 1);

    private final JdbcTemplate jdbc;

    public RateStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every rate on the sheet, by grade then from-date. */
    public List<RateResponse> list(String sheetId) {
        return jdbc.query("SELECT grade, pct, from_date FROM rate WHERE sheet_id = ? ORDER BY grade, from_date",
                (rs, i) -> {
                    LocalDate from = rs.getObject("from_date", LocalDate.class);
                    return RateResponse.of(rs.getString("grade"), rs.getBigDecimal("pct"),
                            START.equals(from) ? null : from);
                }, sheetId);
    }

    /** The grade's percent in force on the date (the rate with the latest from-date on or before it),
     *  or null when no rate of the grade applies then. */
    public BigDecimal find(String sheetId, String grade, LocalDate on) {
        List<BigDecimal> found = jdbc.queryForList(
                "SELECT pct FROM rate WHERE sheet_id = ? AND grade = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1",
                BigDecimal.class, sheetId, grade, on);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Sets the grade's rate from the date (null for from the beginning of time), replacing any there. */
    public void set(String sheetId, String grade, LocalDate from, BigDecimal pct) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, from_date, pct) KEY (sheet_id, grade, from_date) VALUES (?, ?, ?, ?)",
                sheetId, grade, from == null ? START : from, pct);
    }
}

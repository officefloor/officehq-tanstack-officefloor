package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.history.History;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rates for each grade, kept per sheet, each applying from its from-date through its to-date
 *  (if it has one). A rate with no from-date applies from the beginning of time (stored as
 *  {@link #START}). A grade with no rate is absent. */
@Repository
public class RateStore {

    /** The stored from-date of a rate that applies from the beginning of time. */
    private static final LocalDate START = LocalDate.of(1, 1, 1);

    private final JdbcTemplate jdbc;

    /** Where the rates are read from: the rate table, or the rates as they stood at the end of a date. */
    private final String rates;

    @Autowired
    public RateStore(JdbcTemplate jdbc) {
        this(jdbc, "rate");
    }

    private RateStore(JdbcTemplate jdbc, String rates) {
        this.jdbc = jdbc;
        this.rates = rates;
    }

    /** The rates as they stood at the end of the date (see {@link History}), to read only. */
    public RateStore asOf(LocalDate date) {
        return new RateStore(jdbc, History.asOf("rate", "sheet_id, grade, from_date", date));
    }

    /** Every rate on the sheet, by grade then from-date. */
    public List<RateResponse> list(String sheetId) {
        return jdbc.query("SELECT grade, pct, from_date, to_date FROM " + rates + " WHERE sheet_id = ? ORDER BY grade, from_date",
                (rs, i) -> {
                    LocalDate from = rs.getObject("from_date", LocalDate.class);
                    return RateResponse.of(rs.getString("grade"), rs.getBigDecimal("pct"),
                            START.equals(from) ? null : from, rs.getObject("to_date", LocalDate.class));
                }, sheetId);
    }

    /** The grade's percent in force on the date (the rate with the latest from-date on or before it),
     *  or null when no rate of the grade applies then, or that rate ended (its to-date) before it. */
    public BigDecimal find(String sheetId, String grade, LocalDate on) {
        List<BigDecimal> found = jdbc.query(
                "SELECT pct, to_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1",
                (rs, i) -> {
                    LocalDate to = rs.getObject("to_date", LocalDate.class);
                    return to != null && to.isBefore(on) ? null : rs.getBigDecimal("pct");
                }, sheetId, grade, on);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether the grade's rate that would apply on the date (the one with the latest from-date on or
     *  before it) ended (its to-date) before it. */
    public boolean ended(String sheetId, String grade, LocalDate on) {
        List<LocalDate> found = jdbc.query(
                "SELECT to_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1",
                (rs, i) -> rs.getObject("to_date", LocalDate.class), sheetId, grade, on);
        return !found.isEmpty() && found.get(0) != null && found.get(0).isBefore(on);
    }

    /** Whether the grade's rate in force on the date ends before {@code today}: its to-date, or the day
     *  before the grade's next rate starts, whichever is first, is before it. */
    public boolean endsBefore(String sheetId, String grade, LocalDate on, LocalDate today) {
        List<LocalDate> to = jdbc.query(
                "SELECT to_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1",
                (rs, i) -> rs.getObject("to_date", LocalDate.class), sheetId, grade, on);
        if (to.isEmpty()) {
            return false;
        }
        if (to.get(0) != null && to.get(0).isBefore(today)) {
            return true;
        }
        List<LocalDate> next = jdbc.queryForList(
                "SELECT from_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date > ? ORDER BY from_date LIMIT 1",
                LocalDate.class, sheetId, grade, on);
        return !next.isEmpty() && !next.get(0).isAfter(today);
    }

    /** Sets the grade's rate from the date (null for from the beginning of time) through the to-date
     *  (null for no end), replacing any there. */
    public void set(String sheetId, String grade, LocalDate from, LocalDate to, BigDecimal pct) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, from_date, to_date, pct) KEY (sheet_id, grade, from_date) VALUES (?, ?, ?, ?, ?)",
                sheetId, grade, from == null ? START : from, to, pct);
    }
}

package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rate store: percents per grade, each from a date (or from the beginning of time), kept per sheet. */
@Repository
public class Rates {

    private final JdbcTemplate jdbc;

    private final Sheets sheets;

    public Rates(JdbcTemplate jdbc, Sheets sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's rates, by grade then from-date (from the beginning of time first). */
    public List<RateResponse> list(String sheetId) {
        return jdbc.query("SELECT grade, rate, from_date, to_date, version FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST",
                (rs, i) -> {
                    Date from = rs.getDate("from_date");
                    Date to = rs.getDate("to_date");
                    return new RateResponse(rs.getString("grade"), rs.getBigDecimal("rate").setScale(2).toPlainString(),
                            from == null ? "" : from.toLocalDate().toString(), to == null ? "" : to.toLocalDate().toString(),
                            rs.getInt("version"));
                },
                sheetId);
    }

    /** How many levels up a sheet's parents are asked for a rate the sheet has not. */
    public static final int ANCESTOR_LEVELS = 3;

    /** The sheet's ancestors asked for a rate it has not: its parent, then the parent's parent, up to
     *  {@link #ANCESTOR_LEVELS} levels up. */
    public List<String> ancestors(String sheetId) {
        List<String> chain = new java.util.ArrayList<>();
        String at = sheets.parent(sheetId);
        while (at != null && chain.size() < ANCESTOR_LEVELS && !at.equals(sheetId) && !chain.contains(at)) {
            chain.add(at);
            at = sheets.parent(at);
        }
        return chain;
    }

    /** The rates of the sheet's ancestors (see {@link #ancestors(String)}), nearest first. */
    public List<List<RateResponse>> inherited(String sheetId) {
        return ancestors(sheetId).stream().map(this::list).toList();
    }

    /** The rate for a grade on the sheet in force on a date (see {@link #findOwn}); when the sheet has
     *  none, its parent's, then the parent's parent's, up to {@link #ANCESTOR_LEVELS} levels up; null when none has one. */
    public BigDecimal find(String sheetId, String grade, LocalDate date) {
        BigDecimal own = findOwn(sheetId, grade, date);
        if (own != null) {
            return own;
        }
        for (String ancestor : ancestors(sheetId)) {
            BigDecimal rate = findOwn(ancestor, grade, date);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /** The rate for a grade set on the sheet itself in force on a date: the one with the latest from-date on or
     *  before it; null when none applies, or when that one's to-date is before the date. */
    public BigDecimal findOwn(String sheetId, String grade, LocalDate date) {
        Date on = Date.valueOf(date);
        List<BigDecimal> found = jdbc.query("SELECT rate, to_date FROM rate WHERE sheet_id = ? AND grade = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1",
                (rs, i) -> {
                    Date to = rs.getDate("to_date");
                    return to != null && to.before(on) ? null : rs.getBigDecimal("rate");
                }, sheetId, grade, on);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The due on an amount at a rate (a percent), rounded to cents by the claim's rounding; null when there is no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate, RoundingMode rounding) {
        return rate == null ? null : amount.multiply(rate).movePointLeft(2).setScale(2, rounding);
    }

    /** Set the grade's rate from a date (null: from the beginning of time) through a date (null: no
     *  end) as a new version: one already there from the same date is kept as an older version. */
    public void set(String sheetId, String grade, LocalDate from, LocalDate to, BigDecimal rate) {
        Date fromDate = from == null ? null : Date.valueOf(from);
        String sameFrom = fromDate == null ? " AND from_date IS NULL" : " AND from_date = ?";
        Object[] key = fromDate == null ? new Object[] { sheetId, grade } : new Object[] { sheetId, grade, fromDate };
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, rate, to_date)"
                + " SELECT sheet_id, grade, from_date, version, rate, to_date FROM rate WHERE sheet_id = ? AND grade = ?" + sameFrom, key);
        Integer latest = jdbc.queryForObject("SELECT MAX(version) FROM rate WHERE sheet_id = ? AND grade = ?" + sameFrom, Integer.class, key);
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ?" + sameFrom, key);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date, version) VALUES (?, ?, ?, ?, ?, ?)",
                sheetId, grade, rate, fromDate, to == null ? null : Date.valueOf(to), latest == null ? 1 : latest + 1);
        jdbc.update("INSERT INTO rate_history (sheet_id, set_on, grade, rate, from_date, to_date) VALUES (?, (SELECT today FROM sheet WHERE id = ?), ?, ?, ?, ?)",
                sheetId, sheetId, grade, rate, fromDate, to == null ? null : Date.valueOf(to));
    }
}

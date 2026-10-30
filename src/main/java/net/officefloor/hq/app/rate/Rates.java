package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rate store: percents per grade, each from a date (or from the beginning of time), kept per sheet. */
@Repository
public class Rates {

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade then from-date (from the beginning of time first). */
    public List<RateResponse> list(String sheetId) {
        return jdbc.query("SELECT grade, rate, from_date, to_date FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST",
                (rs, i) -> {
                    Date from = rs.getDate("from_date");
                    Date to = rs.getDate("to_date");
                    return new RateResponse(rs.getString("grade"), rs.getBigDecimal("rate").setScale(2).toPlainString(),
                            from == null ? "" : from.toLocalDate().toString(), to == null ? "" : to.toLocalDate().toString());
                },
                sheetId);
    }

    /** The rate for a grade on the sheet in force on a date: the one with the latest from-date on or
     *  before it; null when none applies, or when that one's to-date is before the date. */
    public BigDecimal find(String sheetId, String grade, LocalDate date) {
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
     *  end), replacing one already there from the same date. */
    public void set(String sheetId, String grade, LocalDate from, LocalDate to, BigDecimal rate) {
        Date fromDate = from == null ? null : Date.valueOf(from);
        if (fromDate == null) {
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NULL", sheetId, grade);
        } else {
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date = ?", sheetId, grade, fromDate);
        }
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                sheetId, grade, rate, fromDate, to == null ? null : Date.valueOf(to));
        jdbc.update("INSERT INTO rate_history (sheet_id, set_on, grade, rate, from_date, to_date) VALUES (?, (SELECT today FROM sheet WHERE id = ?), ?, ?, ?, ?)",
                sheetId, sheetId, grade, rate, fromDate, to == null ? null : Date.valueOf(to));
    }
}

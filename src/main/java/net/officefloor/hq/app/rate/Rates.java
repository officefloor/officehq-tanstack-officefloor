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
        return jdbc.query("SELECT grade, rate, from_date FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST",
                (rs, i) -> {
                    Date from = rs.getDate("from_date");
                    return new RateResponse(rs.getString("grade"), rs.getBigDecimal("rate").setScale(2).toPlainString(),
                            from == null ? "" : from.toLocalDate().toString());
                },
                sheetId);
    }

    /** The rate for a grade on the sheet in force on a date: the one with the latest from-date on or
     *  before it; null when none applies. */
    public BigDecimal find(String sheetId, String grade, LocalDate date) {
        List<BigDecimal> found = jdbc.query("SELECT rate FROM rate WHERE sheet_id = ? AND grade = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1",
                (rs, i) -> rs.getBigDecimal("rate"), sheetId, grade, Date.valueOf(date));
        return found.isEmpty() ? null : found.get(0);
    }

    /** The due on an amount at a rate (a percent), rounded half to even to cents; null when there is no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate) {
        return rate == null ? null : amount.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
    }

    /** Set the grade's rate from a date (null: from the beginning of time), replacing one already there. */
    public void set(String sheetId, String grade, LocalDate from, BigDecimal rate) {
        Date fromDate = from == null ? null : Date.valueOf(from);
        if (fromDate == null) {
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NULL", sheetId, grade);
        } else {
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date = ?", sheetId, grade, fromDate);
        }
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date) VALUES (?, ?, ?, ?)",
                sheetId, grade, rate, fromDate);
    }
}

package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rate store: one percent per grade, kept per sheet. */
@Repository
public class Rates {

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade. A grade with no rate is absent. */
    public List<RateResponse> list(String sheetId) {
        return jdbc.query("SELECT grade, rate FROM rate WHERE sheet_id = ? ORDER BY grade",
                (rs, i) -> new RateResponse(rs.getString("grade"), rs.getBigDecimal("rate").setScale(2).toPlainString()),
                sheetId);
    }

    /** The rate for a grade on the sheet; null when the grade has none. */
    public BigDecimal find(String sheetId, String grade) {
        List<BigDecimal> found = jdbc.query("SELECT rate FROM rate WHERE sheet_id = ? AND grade = ?",
                (rs, i) -> rs.getBigDecimal("rate"), sheetId, grade);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The due on an amount at a rate (a percent), rounded half up to cents; null when there is no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate) {
        return rate == null ? null : amount.multiply(rate).movePointLeft(2).setScale(2, RoundingMode.HALF_UP);
    }

    public void set(String sheetId, String grade, BigDecimal rate) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, rate) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                sheetId, grade, rate);
    }
}

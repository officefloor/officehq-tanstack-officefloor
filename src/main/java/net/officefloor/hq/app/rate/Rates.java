package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
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

    public void set(String sheetId, String grade, BigDecimal rate) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, rate) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                sheetId, grade, rate);
    }
}

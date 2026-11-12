package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rate for each grade, kept per sheet. A grade with no rate is absent. */
@Repository
public class RateStore {

    private final JdbcTemplate jdbc;

    public RateStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<RateResponse> list(String sheetId) {
        return jdbc.query("SELECT grade, pct FROM rate WHERE sheet_id = ? ORDER BY grade",
                (rs, i) -> RateResponse.of(rs.getString("grade"), rs.getBigDecimal("pct")), sheetId);
    }

    /** The grade's percent, or null when the grade has no rate. */
    public BigDecimal find(String sheetId, String grade) {
        List<BigDecimal> found = jdbc.queryForList("SELECT pct FROM rate WHERE sheet_id = ? AND grade = ?",
                BigDecimal.class, sheetId, grade);
        return found.isEmpty() ? null : found.get(0);
    }

    public void set(String sheetId, String grade, BigDecimal pct) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, pct) KEY (sheet_id, grade) VALUES (?, ?, ?)", sheetId, grade,
                pct);
    }
}

package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** Rates by sheet, one per grade at most. */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, i) -> new Rate(rs.getString("sheet_id"),
            rs.getString("grade"), rs.getBigDecimal("pct"));

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT * FROM rate WHERE sheet_id = ? ORDER BY grade", ROW, sheetId);
    }

    /** Set the grade's rate, replacing any it had. */
    public Rate set(String sheetId, String grade, BigDecimal pct) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, pct) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                sheetId, grade, pct);
        return new Rate(sheetId, grade, pct);
    }
}

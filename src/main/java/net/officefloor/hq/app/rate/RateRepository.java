package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, n) -> new Rate(rs.getString("sheet_id"),
            rs.getString("grade"), rs.getBigDecimal("pct").setScale(2).toPlainString());

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade. A grade without a rate is not listed. */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, pct FROM rate WHERE sheet_id = ? ORDER BY grade",
                ROW, sheetId);
    }

    /** The grade's rate on the sheet; null when it has none. */
    public Rate find(String sheetId, String grade) {
        return jdbc.query("SELECT sheet_id, grade, pct FROM rate WHERE sheet_id = ? AND grade = ?",
                ROW, sheetId, grade).stream().findFirst().orElse(null);
    }

    /** Set (or replace) the grade's rate on the sheet. */
    public Rate set(String sheetId, String grade, BigDecimal pct, String user) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, pct, set_by, set_at) KEY (sheet_id, grade) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, grade, pct, user);
        return new Rate(sheetId, grade, pct.toPlainString());
    }
}

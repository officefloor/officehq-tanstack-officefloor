package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** The sheet's rates, by grade. */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT grade, pct FROM rate WHERE sheet_id = ? ORDER BY grade",
                (rs, i) -> new Rate(rs.getString("grade"), pct(rs.getBigDecimal("pct"))), sheetId);
    }

    /** Set a grade's rate, replacing the one it had. */
    public Rate set(String sheetId, String grade, BigDecimal pct) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, pct) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                sheetId, grade, pct);
        return new Rate(grade, pct(pct));
    }

    /** A percent as shown: two decimals, a dot, no grouping. */
    static String pct(BigDecimal pct) {
        return pct.setScale(2).toPlainString();
    }
}

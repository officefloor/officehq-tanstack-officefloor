package net.officefloor.hq.app.rate;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rate for each grade of each sheet; a grade has at most one. */
@Repository
public class Rates {

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Rate> onSheet(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, pct FROM rate WHERE sheet_id = ? ORDER BY grade",
                (rs, i) -> new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("pct")),
                sheetId);
    }

    /** Set the grade's rate, replacing the one it had. */
    public void save(Rate rate) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, pct) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct());
    }
}

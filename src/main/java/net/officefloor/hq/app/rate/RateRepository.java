package net.officefloor.hq.app.rate;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The rates of each sheet, one per grade at most, listed by grade. */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, i) -> new Rate(rs.getString("sheet_id"),
            rs.getString("grade"), rs.getBigDecimal("pct").setScale(2));

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Rate> findBySheet(String sheetId) {
        return jdbc.query("SELECT * FROM rate WHERE sheet_id = ? ORDER BY grade", ROW, sheetId);
    }

    /** Sets the grade's rate, replacing the one it had. */
    public void save(Rate rate) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, pct) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct());
    }
}

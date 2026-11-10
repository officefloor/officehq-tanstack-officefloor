package net.officefloor.hq.app.rate;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The rates store: at most one rate per grade, kept per sheet. */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, n) -> new Rate(rs.getString("sheet_id"),
            rs.getString("grade"), rs.getBigDecimal("pct"));

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade. */
    public List<Rate> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, pct FROM rate WHERE sheet_id = ? ORDER BY grade",
                ROW, sheetId);
    }

    /** Set the grade's rate, replacing any it had. */
    public void save(Rate rate) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, pct) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct());
    }
}

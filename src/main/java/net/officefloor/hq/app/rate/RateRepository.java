package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The rate for each grade on each sheet; a grade without one has no rate. */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, n) -> new Rate(rs.getString("sheet_id"), rs.getString("grade"),
            rs.getBigDecimal("rate"));

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade. */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, rate FROM rate WHERE sheet_id = ? ORDER BY grade", ROW, sheetId);
    }

    /** Set (or replace) a grade's rate. */
    public Rate set(String sheetId, String grade, BigDecimal rate) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, rate) KEY (sheet_id, grade) VALUES (?, ?, ?)", sheetId, grade,
                rate);
        return new Rate(sheetId, grade, rate);
    }
}

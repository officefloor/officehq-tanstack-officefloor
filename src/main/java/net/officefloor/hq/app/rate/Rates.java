package net.officefloor.hq.app.rate;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The store of rates, one per grade, kept per sheet. */
@Repository
public class Rates {

    private static final RowMapper<Rate> ROW = (rs, n) -> new Rate(rs.getString("sheet_id"), rs.getString("grade"),
            rs.getBigDecimal("rate"));

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade. */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT * FROM rate WHERE sheet_id = ? ORDER BY grade", ROW, sheetId);
    }

    public void save(Rate rate) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, rate) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate());
    }
}

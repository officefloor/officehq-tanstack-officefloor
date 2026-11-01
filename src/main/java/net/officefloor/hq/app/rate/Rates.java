package net.officefloor.hq.app.rate;

import java.math.RoundingMode;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The rates kept per sheet, one per grade. */
@Repository
public class Rates {

    private static final RowMapper<Rate> ROW = (rs, i) -> new Rate(rs.getString("grade"),
            rs.getBigDecimal("pct").setScale(2, RoundingMode.UNNECESSARY).toPlainString());

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every grade's rate on the sheet, by grade. */
    public List<Rate> list(String sheet) {
        return jdbc.query("SELECT grade, pct FROM rate WHERE sheet = ? ORDER BY grade", ROW, sheet);
    }

    public void set(String sheet, RateInput input) {
        jdbc.update("MERGE INTO rate (sheet, grade, pct) KEY (sheet, grade) VALUES (?, ?, ?)", sheet, input.grade(),
                input.pct());
    }
}

package net.officefloor.hq.app.rate;

import java.sql.Date;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The rates of each sheet, one per grade and from-date at most, listed by grade then from-date
 *  (the one with no from-date first). */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, i) -> {
        Date from = rs.getDate("from_date");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("pct").setScale(2),
                from == null ? null : from.toLocalDate());
    };

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Rate> findBySheet(String sheetId) {
        return jdbc.query("SELECT * FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /** Sets the grade's rate for its from-date, replacing the one it had. */
    public void save(Rate rate) {
        Date from = rate.from() == null ? null : Date.valueOf(rate.from());
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                rate.sheetId(), rate.grade(), from);
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date) VALUES (?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct(), from);
    }
}

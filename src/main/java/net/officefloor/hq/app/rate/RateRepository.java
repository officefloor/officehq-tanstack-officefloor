package net.officefloor.hq.app.rate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored rates, one per grade per sheet. A grade with no row has no rate. */
@Repository
public class RateRepository {

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, rate FROM rate WHERE sheet_id = ? ORDER BY grade",
                RateRepository::map, sheetId);
    }

    public Rate save(Rate rate) {
        jdbc.update("MERGE INTO rate (sheet_id, grade, rate) KEY (sheet_id, grade) VALUES (?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate());
        return rate;
    }

    private static Rate map(ResultSet rs, int n) throws SQLException {
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate").setScale(2));
    }
}

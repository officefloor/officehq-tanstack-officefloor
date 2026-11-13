package net.officefloor.hq.app.rate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade. */
    public List<Rate> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, rate FROM rate WHERE sheet_id = ? ORDER BY grade",
                RateRepository::map, sheetId);
    }

    /** Set the grade's rate, replacing any it had; the rate as stored. */
    public Rate save(Rate rate) {
        int updated = jdbc.update("UPDATE rate SET rate = ? WHERE sheet_id = ? AND grade = ?",
                rate.rate(), rate.sheetId(), rate.grade());
        if (updated == 0) {
            jdbc.update("INSERT INTO rate (sheet_id, grade, rate) VALUES (?, ?, ?)",
                    rate.sheetId(), rate.grade(), rate.rate());
        }
        return rate;
    }

    private static Rate map(ResultSet rs, int n) throws SQLException {
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate").setScale(2));
    }
}

package net.officefloor.hq.app.rate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class RateRepository {

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade and then from-date (from the start first). */
    public List<Rate> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, rate, from_date, to_date FROM rate WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST", RateRepository::map, sheetId);
    }

    /** Set the grade's rate for its from-date, replacing any it had (and its to-date) for that from-date; the rate as stored. */
    public Rate save(Rate rate) {
        int updated = rate.from() == null
                ? jdbc.update("UPDATE rate SET rate = ?, to_date = ? WHERE sheet_id = ? AND grade = ? AND from_date IS NULL",
                        rate.rate(), rate.to(), rate.sheetId(), rate.grade())
                : jdbc.update("UPDATE rate SET rate = ?, to_date = ? WHERE sheet_id = ? AND grade = ? AND from_date = ?",
                        rate.rate(), rate.to(), rate.sheetId(), rate.grade(), rate.from());
        if (updated == 0) {
            jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                    rate.sheetId(), rate.grade(), rate.rate(), rate.from(), rate.to());
        }
        return rate;
    }

    private static Rate map(ResultSet rs, int n) throws SQLException {
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate").setScale(2),
                rs.getObject("from_date", LocalDate.class), rs.getObject("to_date", LocalDate.class));
    }
}

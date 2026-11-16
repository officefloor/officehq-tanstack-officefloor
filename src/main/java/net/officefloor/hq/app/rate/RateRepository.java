package net.officefloor.hq.app.rate;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored rates, one per grade and from-date per sheet. A grade with no row has no rate. */
@Repository
public class RateRepository {

    private static final String COLUMNS = "sheet_id, grade, rate, from_date, to_date";

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade, then from-date (one with none first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT " + COLUMNS + " FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST",
                RateRepository::map, sheetId);
    }

    /** The grade's rate in force on a date — the latest from-date on or before it, a rate with none applying
     *  from the beginning of time — or null when no rate applies, or that rate's to-date is before the date. */
    public Rate find(String sheetId, String grade, LocalDate on) {
        List<Rate> found = jdbc.query("SELECT " + COLUMNS + " FROM rate WHERE sheet_id = ? AND grade = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1",
                RateRepository::map, sheetId, grade, Date.valueOf(on));
        if (found.isEmpty() || (found.get(0).to() != null && found.get(0).to().isBefore(on))) {
            return null;
        }
        return found.get(0);
    }

    /** Stores the rate, replacing the one the grade had for the same from-date. */
    public Rate save(Rate rate) {
        Date from = rate.from() == null ? null : Date.valueOf(rate.from());
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                rate.sheetId(), rate.grade(), from);
        jdbc.update("INSERT INTO rate (" + COLUMNS + ") VALUES (?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate(), from, rate.to() == null ? null : Date.valueOf(rate.to()));
        return rate;
    }

    private static Rate map(ResultSet rs, int n) throws SQLException {
        Date from = rs.getDate("from_date");
        Date to = rs.getDate("to_date");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate").setScale(2),
                from == null ? null : from.toLocalDate(), to == null ? null : to.toLocalDate());
    }
}

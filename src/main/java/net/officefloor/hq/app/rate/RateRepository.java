package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The rates store: at most one rate per grade and from-date, kept per sheet. */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, n) -> new Rate(rs.getString("sheet_id"),
            rs.getString("grade"), rs.getBigDecimal("pct"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class));

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's rates, by grade then from-date (none first). */
    public List<Rate> findAll(String sheetId) {
        return jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date FROM rate WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /** Set the grade's rate from its from-date, replacing any it had for that from-date. */
    public void save(Rate rate) {
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                rate.sheetId(), rate.grade(), rate.from());
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct(), rate.from(), rate.to());
        jdbc.update("INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, set_on)"
                + " VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                rate.sheetId(), rate.grade(), rate.pct(), rate.from(), rate.to(), rate.sheetId());
    }

    /**
     * The sheet's rates as they stood at the end of {@code day}: for each grade and from-date, the
     * last rate set for them while the sheet's today was on or before it.
     */
    public List<Rate> findAsOf(String sheetId, LocalDate day) {
        Map<String, Rate> latest = new LinkedHashMap<>();
        jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date FROM rate_history WHERE sheet_id = ?"
                + " AND set_on <= ? ORDER BY id", (rs) -> {
                    Rate rate = ROW.mapRow(rs, 0);
                    latest.put(rate.grade() + "|" + rate.from(), rate);
                }, sheetId, java.sql.Date.valueOf(day));
        return new ArrayList<>(latest.values());
    }
}

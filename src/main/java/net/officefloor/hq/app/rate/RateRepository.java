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

    /**
     * The sheet's rates, by grade then from-date (none first), then those of its parent sheet and on
     * up to {@link Rate#MAX_LEVEL} levels, each at its level.
     */
    public List<Rate> findAll(String sheetId) {
        List<Rate> all = new ArrayList<>(findOwn(sheetId));
        List<String> ancestors = ancestors(sheetId);
        for (int i = 0; i < ancestors.size(); i++) {
            int level = i + 1;
            findOwn(ancestors.get(i)).forEach((r) -> all.add(r.up(level)));
        }
        return all;
    }

    /** The sheet's parent, its parent's parent and so on, up to {@link Rate#MAX_LEVEL} of them. */
    private List<String> ancestors(String sheetId) {
        List<String> ancestors = new ArrayList<>();
        String at = sheetId;
        while (ancestors.size() < Rate.MAX_LEVEL) {
            List<String> parent = jdbc.query("SELECT parent_id FROM sheet WHERE id = ?",
                    (rs, n) -> rs.getString("parent_id"), at);
            if (parent.isEmpty() || parent.get(0) == null) break;
            at = parent.get(0);
            ancestors.add(at);
        }
        return ancestors;
    }

    /** The rates set on the sheet itself, by grade then from-date (none first). */
    private List<Rate> findOwn(String sheetId) {
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
     * last rate set for them while the sheet's today was on or before it; then its parent sheets'
     * (as they are named now) as they stood at the end of that day, each at its level.
     */
    public List<Rate> findAsOf(String sheetId, LocalDate day) {
        List<Rate> all = new ArrayList<>(findOwnAsOf(sheetId, day));
        List<String> ancestors = ancestors(sheetId);
        for (int i = 0; i < ancestors.size(); i++) {
            int level = i + 1;
            findOwnAsOf(ancestors.get(i), day).forEach((r) -> all.add(r.up(level)));
        }
        return all;
    }

    private List<Rate> findOwnAsOf(String sheetId, LocalDate day) {
        Map<String, Rate> latest = new LinkedHashMap<>();
        jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date FROM rate_history WHERE sheet_id = ?"
                + " AND set_on <= ? ORDER BY id", (rs) -> {
                    Rate rate = ROW.mapRow(rs, 0);
                    latest.put(rate.grade() + "|" + rate.from(), rate);
                }, sheetId, java.sql.Date.valueOf(day));
        return new ArrayList<>(latest.values());
    }
}

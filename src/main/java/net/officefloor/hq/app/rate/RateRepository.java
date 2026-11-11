package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * The rates store: per grade and from-date, the newest version of its rate (the one in force) and the
 * older versions it replaced, kept per sheet.
 */
@Repository
public class RateRepository {

    private static final RowMapper<Rate> ROW = (rs, n) -> new Rate(rs.getString("sheet_id"),
            rs.getString("grade"), rs.getBigDecimal("pct"), rs.getObject("from_date", LocalDate.class),
            rs.getObject("to_date", LocalDate.class), 0, rs.getInt("version"));

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
        return jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, version FROM rate WHERE sheet_id = ?"
                + " ORDER BY grade, from_date NULLS FIRST", ROW, sheetId);
    }

    /**
     * Set the grade's rate from its from-date as a new version: any it had for that from-date is kept
     * as an older version. Returns the rate at its version.
     */
    public Rate save(Rate rate) {
        String where = " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?";
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date)"
                + " SELECT sheet_id, grade, from_date, version, pct, to_date FROM rate" + where,
                rate.sheetId(), rate.grade(), rate.from());
        Integer last = jdbc.queryForObject("SELECT MAX(version) FROM rate" + where, Integer.class,
                rate.sheetId(), rate.grade(), rate.from());
        Rate saved = rate.withVersion(last == null ? 1 : last + 1);
        jdbc.update("DELETE FROM rate" + where, rate.sheetId(), rate.grade(), rate.from());
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version) VALUES (?, ?, ?, ?, ?, ?)",
                saved.sheetId(), saved.grade(), saved.pct(), saved.from(), saved.to(), saved.version());
        jdbc.update("INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, set_on)"
                + " VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                rate.sheetId(), rate.grade(), rate.pct(), rate.from(), rate.to(), rate.sheetId());
        return saved;
    }

    /**
     * The sheet's rates as they stood at the end of {@code day}: for each grade and from-date, the
     * last rate set for them while the sheet's today was on or before it, its version counting those
     * set for them by then; then its parent sheets'
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
        jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, 1 AS version FROM rate_history WHERE sheet_id = ?"
                + " AND set_on <= ? ORDER BY id", (rs) -> {
                    Rate rate = ROW.mapRow(rs, 0);
                    String key = rate.grade() + "|" + rate.from();
                    Rate before = latest.get(key);
                    latest.put(key, before == null ? rate : rate.withVersion(before.version() + 1));
                }, sheetId, java.sql.Date.valueOf(day));
        return new ArrayList<>(latest.values());
    }
}

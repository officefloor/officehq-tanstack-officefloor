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
            rs.getObject("to_date", LocalDate.class), 0, rs.getInt("version"),
            rs.getObject("added_after", Long.class), null);

    private final JdbcTemplate jdbc;

    public RateRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /**
     * The sheet's rates, by grade then from-date (none first), then those of its parent sheet and on
     * up to {@link Rate#MAX_LEVEL} levels, then those of the sheet it was cloned from and on up its
     * own (see {@link #ancestors}), each at its level.
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

    /**
     * The sheets a rate is looked for on after the sheet itself, in order: its parent, its parent's
     * parent and so on, up to {@link Rate#MAX_LEVEL} of them; then, when it was cloned, the sheet it
     * was cloned from and that sheet's own (its parents, then the sheet it was cloned from, and so on).
     */
    private List<String> ancestors(String sheetId) {
        List<String> ancestors = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        String sheet = sheetId;
        while (sheet != null) {
            String at = sheet;
            for (int up = 0; up < Rate.MAX_LEVEL; up++) {
                at = column("parent_id", at);
                if (at == null || !seen.add(at)) break;
                ancestors.add(at);
            }
            sheet = column("cloned_from", sheet);
            if (sheet == null || !seen.add(sheet)) break;
            ancestors.add(sheet);
        }
        return ancestors;
    }

    private String column(String column, String sheetId) {
        List<String> found = jdbc.query("SELECT " + column + " FROM sheet WHERE id = ?",
                (rs, n) -> rs.getString(column), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /**
     * The rates set on the sheet itself, by grade then from-date (none first), each with the older
     * versions it replaced (for the claims a forward-only version does not apply to).
     */
    private List<Rate> findOwn(String sheetId) {
        Map<String, Rate> older = new LinkedHashMap<>();
        jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, version, added_after FROM rate_version"
                + " WHERE sheet_id = ? ORDER BY version, id", (rs) -> {
                    Rate rate = ROW.mapRow(rs, 0);
                    String key = key(rate);
                    older.put(key, rate.withHistory(rate.addedAfter(), older.get(key)));
                }, sheetId);
        return jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, version, added_after FROM rate"
                + " WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST", ROW, sheetId).stream()
                .map((r) -> r.withHistory(r.addedAfter(), older.get(key(r))))
                .toList();
    }

    private static String key(Rate rate) {
        return rate.grade() + "|" + rate.from();
    }

    /**
     * Set the grade's rate from its from-date as a new version: any it had for that from-date is kept
     * as an older version. A rate with a from-date on or after the sheet's today is forward-only: it
     * applies only to claims added after now (those above the highest claim id). Returns the rate at
     * its version.
     */
    public Rate save(Rate rate) {
        String where = " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?";
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date, added_after)"
                + " SELECT sheet_id, grade, from_date, version, pct, to_date, added_after FROM rate" + where,
                rate.sheetId(), rate.grade(), rate.from());
        Integer last = jdbc.queryForObject("SELECT MAX(version) FROM rate" + where, Integer.class,
                rate.sheetId(), rate.grade(), rate.from());
        LocalDate today = jdbc.queryForObject("SELECT today FROM sheet WHERE id = ?", LocalDate.class, rate.sheetId());
        Long addedAfter = rate.from() != null && today != null && !rate.from().isBefore(today)
                ? jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM claim", Long.class)
                : null;
        Rate saved = rate.withVersion(last == null ? 1 : last + 1).withHistory(addedAfter, null);
        jdbc.update("DELETE FROM rate" + where, rate.sheetId(), rate.grade(), rate.from());
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version, added_after)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                saved.sheetId(), saved.grade(), saved.pct(), saved.from(), saved.to(), saved.version(), addedAfter);
        jdbc.update("INSERT INTO rate_history (sheet_id, grade, pct, from_date, to_date, set_on, added_after)"
                + " VALUES (?, ?, ?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct(), rate.from(), rate.to(), today, addedAfter);
        return saved;
    }

    /**
     * Withdraw the newest version of the grade's rate from its from-date (null: from the beginning of
     * time): the version before it, when there is one, is in force again; with none the rate is gone.
     * Its last record in the rate history goes with it. False when the sheet has no such rate.
     */
    public boolean withdraw(String sheetId, String grade, LocalDate from) {
        String where = " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?";
        if (jdbc.update("DELETE FROM rate" + where, sheetId, grade, from) == 0) return false;
        List<Long> older = jdbc.query("SELECT id FROM rate_version" + where + " ORDER BY version DESC, id DESC LIMIT 1",
                (rs, n) -> rs.getLong("id"), sheetId, grade, from);
        if (!older.isEmpty()) {
            jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version, added_after)"
                    + " SELECT sheet_id, grade, pct, from_date, to_date, version, added_after FROM rate_version WHERE id = ?",
                    older.get(0));
            jdbc.update("DELETE FROM rate_version WHERE id = ?", older.get(0));
        }
        List<Long> history = jdbc.query("SELECT id FROM rate_history" + where + " ORDER BY id DESC LIMIT 1",
                (rs, n) -> rs.getLong("id"), sheetId, grade, from);
        if (!history.isEmpty()) jdbc.update("DELETE FROM rate_history WHERE id = ?", history.get(0));
        return true;
    }

    /**
     * The sheet's rates as they stood at the end of {@code day}: for each grade and from-date, the
     * last rate set for them while the sheet's today was on or before it, its version counting those
     * set for them by then; then its parent sheets' (as they are named now), each at its level and read
     * the same way: those set while the parent's own today was on or before {@code day}.
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
        jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, 1 AS version, added_after FROM rate_history"
                + " WHERE sheet_id = ? AND set_on <= ? ORDER BY id", (rs) -> {
                    Rate rate = ROW.mapRow(rs, 0);
                    String key = key(rate);
                    Rate before = latest.get(key);
                    latest.put(key, before == null ? rate
                            : rate.withVersion(before.version() + 1).withHistory(rate.addedAfter(), before));
                }, sheetId, java.sql.Date.valueOf(day));
        return new ArrayList<>(latest.values());
    }
}

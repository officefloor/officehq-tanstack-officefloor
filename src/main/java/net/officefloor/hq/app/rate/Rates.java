package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** The rates for each grade of each sheet; a grade has at most one in force per from-date (or none), the
 *  newest of its versions. */
@Repository
public class Rates {

    private final JdbcTemplate jdbc;

    public Rates(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** Every rate on the sheet, by grade, then by from-date (the one with none first), each with its older versions. */
    public List<Rate> onSheet(String sheetId) {
        Map<String, List<Rate.Version>> earlier = new HashMap<>();
        jdbc.query("SELECT grade, from_date, pct, to_date, forward_after FROM rate_version WHERE sheet_id = ? ORDER BY version DESC",
                (RowCallbackHandler) rs -> earlier.computeIfAbsent(rs.getString("grade") + "/" + rs.getDate("from_date"),
                        k -> new ArrayList<>()).add(new Rate.Version(rs.getBigDecimal("pct"), date(rs.getDate("to_date")),
                                forwardAfter(rs))),
                sheetId);
        return jdbc.query("SELECT sheet_id, grade, pct, from_date, to_date, version, forward_after FROM rate WHERE sheet_id = ? ORDER BY grade, from_key",
                (rs, i) -> new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("pct"),
                        date(rs.getDate("from_date")), date(rs.getDate("to_date")), rs.getInt("version"), forwardAfter(rs),
                        earlier.getOrDefault(rs.getString("grade") + "/" + rs.getDate("from_date"), List.of())),
                sheetId);
    }

    private static LocalDate date(Date date) {
        return date != null ? date.toLocalDate() : null;
    }

    private static Long forwardAfter(ResultSet rs) throws SQLException {
        long after = rs.getLong("forward_after");
        return rs.wasNull() ? null : after;
    }

    /** How many levels of parent sheets a grade's rate is looked for in. */
    public static final int PARENT_LEVELS = 3;

    /** The grade's rate in force on `date`: the sheet's own (see {@link #ownPct}), else its parent's
     *  then, else the parent's parent's, up to {@link #PARENT_LEVELS} levels up; null when none has one. */
    public BigDecimal pct(String sheetId, String grade, LocalDate date) {
        return pct(sheetId, grade, date, null);
    }

    /** The grade's rate that applies on `date` to the claim with sequence `seq` (see {@link #ownPct}): as
     *  {@link #pct(String, String, LocalDate)}, passing over each forward-only rate saved after the claim
     *  was added. Null `seq` (a claim not yet added) passes over none. */
    public BigDecimal pct(String sheetId, String grade, LocalDate date, Long seq) {
        String sheet = sheetId;
        for (int level = 0; sheet != null && level <= PARENT_LEVELS; level++) {
            BigDecimal pct = ownPct(sheet, grade, date, seq);
            if (pct != null) return pct;
            sheet = parent(sheet);
        }
        return null;
    }

    /** The sheet's parent sheet id, or null when it names none. */
    public String parent(String sheetId) {
        return jdbc.query("SELECT parent_id FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getString("parent_id"), sheetId)
                .stream().filter(p -> p != null).findFirst().orElse(null);
    }

    /** The sheet's ancestors' rates, nearest first: each parent sheet up to {@link #PARENT_LEVELS}
     *  levels up, with every rate set on it. */
    public List<Inherited> inherited(String sheetId) {
        List<Inherited> inherited = new ArrayList<>();
        String sheet = parent(sheetId);
        for (int level = 1; sheet != null && level <= PARENT_LEVELS; level++) {
            inherited.add(new Inherited(level, sheet, onSheet(sheet)));
            sheet = parent(sheet);
        }
        return inherited;
    }

    /** The rates of an ancestor sheet `level` levels up (1 for the parent). */
    public record Inherited(int level, String sheetId, List<Rate> rates) {
    }

    /** The grade's rate in force on `date` set on the sheet itself: the one with the latest from-date
     *  on or before it, or null when there is none or that one's to-date is before `date`. With a claim's
     *  sequence `seq`, the rate that applies to that claim: a forward-only version saved after the claim
     *  was added is passed over for the newest version of its from-date that applies, and a from-date
     *  with none that applies is passed over for the one before. */
    public BigDecimal ownPct(String sheetId, String grade, LocalDate date, Long seq) {
        String applies = seq == null ? "" : " AND (forward_after IS NULL OR forward_after < ?)";
        String versions = seq == null ? "SELECT from_key, version, pct, to_date FROM rate WHERE sheet_id = ? AND grade = ?" + applies
                : "SELECT from_key, version, pct, to_date FROM rate WHERE sheet_id = ? AND grade = ?" + applies
                        + " UNION ALL SELECT from_key, version, pct, to_date FROM rate_version WHERE sheet_id = ? AND grade = ?" + applies;
        List<Object> args = new ArrayList<>(List.of(sheetId, grade));
        if (seq != null) args.addAll(List.of(seq, sheetId, grade, seq));
        args.add(Date.valueOf(date));
        return jdbc.query("SELECT pct, to_date FROM (" + versions + ") v WHERE from_key <= ?"
                + " ORDER BY from_key DESC, version DESC LIMIT 1",
                (rs, i) -> {
                    Date to = rs.getDate("to_date");
                    return to != null && to.toLocalDate().isBefore(date) ? null : rs.getBigDecimal("pct");
                }, args.toArray())
                .stream().filter(p -> p != null).findFirst().orElse(null);
    }

    /** Set the grade's rate from its from-date as its newest version: the one it had for that from-date
     *  is kept as an older version. Saved with a from-date on or after the sheet's `today`, it is
     *  forward-only: it applies only to the claims added from now on (see {@link Rate#forwardAfter}). */
    public void save(Rate rate, LocalDate today) {
        Date from = rate.from() != null ? Date.valueOf(rate.from()) : null;
        Long forwardAfter = rate.from() != null && today != null && !rate.from().isBefore(today)
                ? jdbc.queryForObject("SELECT COALESCE(MAX(id), 0) FROM PUBLIC.claim", Long.class) : null;
        Integer current = jdbc.query("SELECT version FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                (rs, i) -> rs.getInt("version"), rate.sheetId(), rate.grade(), from).stream().findFirst().orElse(null);
        if (current != null) {
            jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date, forward_after) "
                    + "SELECT sheet_id, grade, from_date, version, pct, to_date, forward_after FROM rate "
                    + "WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                    rate.sheetId(), rate.grade(), from);
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                    rate.sheetId(), rate.grade(), from);
        }
        jdbc.update("INSERT INTO rate (sheet_id, grade, pct, from_date, to_date, version, forward_after) VALUES (?, ?, ?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.pct(), from, rate.to() != null ? Date.valueOf(rate.to()) : null,
                current != null ? current + 1 : 1, forwardAfter);
    }
}

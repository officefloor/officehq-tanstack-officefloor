package net.officefloor.hq.app.rate;

import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * The rates kept per sheet: per grade, one for each from-date (and at most one with none), the newest of its versions;
 * the older versions it replaced are kept in rate_version. A sheet is given its
 * parents' rates too, up to {@link Rate#LEVELS} up, each at its level; a cloned sheet then the rates of the sheet it
 * was cloned from, as that sheet is given them, further up.
 */
@Repository
public class Rates {

    private static final RowMapper<Rate> ROW = (rs, i) -> new Rate(rs.getString("grade"),
            rs.getBigDecimal("pct").setScale(2, RoundingMode.UNNECESSARY).toPlainString(),
            rs.getDate("from_date") == null ? null : rs.getDate("from_date").toLocalDate().toString(),
            rs.getDate("to_date") == null ? null : rs.getDate("to_date").toLocalDate().toString(), 0,
            rs.getInt("version"), rs.getObject("forward_after", Long.class), null);

    private final JdbcTemplate jdbc;

    private final Settings settings;

    private final SheetClock clock;

    public Rates(JdbcTemplate jdbc, Settings settings, SheetClock clock) {
        this.jdbc = jdbc;
        this.settings = settings;
        this.clock = clock;
    }

    /**
     * Every rate on the sheet, its own by grade then from-date (the one with none first), then its parents' by level,
     * then (for a cloned sheet) those of the sheet it was cloned from, its parents' and source's in turn, each a
     * {@link Rate#SOURCE_LEVELS} further up.
     */
    public List<Rate> list(String sheet) {
        List<Rate> all = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int base = 0; !sheet.isEmpty() && seen.add(sheet); base += Rate.SOURCE_LEVELS) {
            for (Rate rate : own(sheet)) {
                all.add(rate.up(base));
            }
            String parent = settings.parent(sheet);
            for (int level = 1; level <= Rate.LEVELS && !parent.isEmpty(); level++) {
                for (Rate rate : own(parent)) {
                    all.add(rate.up(base + level));
                }
                parent = settings.parent(parent);
            }
            sheet = source(sheet);
        }
        return all;
    }

    /** The id of the sheet the sheet was cloned from, or "" when it was not cloned. */
    public String source(String sheet) {
        List<String> found = jdbc.queryForList("SELECT source FROM sheet_source WHERE child = ?", String.class, sheet);
        return found.isEmpty() ? "" : found.get(0);
    }

    /**
     * The rates kept on the sheet itself, by grade then from-date (the one with none first); a forward-only one with
     * the versions it replaced, back to one that applies to every claim.
     */
    public List<Rate> own(String sheet) {
        List<Rate> own = jdbc.query("SELECT grade, pct, from_date, to_date, version, forward_after FROM rate WHERE sheet = ?"
                + " ORDER BY grade, from_key", ROW, sheet);
        if (own.stream().noneMatch((r) -> r.after() != null)) {
            return own;
        }
        List<Rate> older = jdbc.query("SELECT grade, pct, from_date, to_date, version, forward_after FROM rate_version"
                + " WHERE sheet = ? ORDER BY version DESC", ROW, sheet);
        return own.stream().map((r) -> withBefore(r, older)).toList();
    }

    /** The rate with the version it replaced (and so on back) while it is forward-only. */
    private static Rate withBefore(Rate rate, List<Rate> older) {
        if (rate.after() == null) {
            return rate;
        }
        Rate before = older.stream()
                .filter((o) -> o.grade().equals(rate.grade()) && Objects.equals(o.from(), rate.from())
                        && o.version() < rate.version())
                .findFirst().map((o) -> withBefore(o, older)).orElse(null);
        return new Rate(rate.grade(), rate.pct(), rate.from(), rate.to(), rate.level(), rate.version(), rate.after(),
                before);
    }

    /**
     * Sets the grade's rate from the from-date (through its to-date) as a new version: the one already there for that
     * grade and from-date is kept as an older version. With a from-date on or after the sheet's today it is
     * forward-only: it applies only to claims added after it.
     */
    public void set(String sheet, RateInput input) {
        Date from = input.from() == null ? null : Date.valueOf(input.from());
        Date to = input.to() == null ? null : Date.valueOf(input.to());
        Long after = input.from() == null || input.from().isBefore(clock.today(sheet)) ? null
                : jdbc.queryForObject("SELECT NEXT VALUE FOR claim_added_seq", Long.class);
        jdbc.update("INSERT INTO rate_version (sheet, grade, from_date, version, pct, to_date, forward_after) SELECT"
                + " sheet, grade, from_date, version, pct, to_date, forward_after FROM rate WHERE sheet = ? AND grade = ?"
                + " AND from_date IS NOT DISTINCT FROM ?", sheet, input.grade(), from);
        if (jdbc.update(
                "UPDATE rate SET pct = ?, to_date = ?, forward_after = ?, version = version + 1 WHERE sheet = ?"
                        + " AND grade = ? AND from_date IS NOT DISTINCT FROM ?",
                input.pct(), to, after, sheet, input.grade(), from) == 0) {
            jdbc.update("INSERT INTO rate (sheet, grade, pct, from_date, to_date, forward_after) VALUES (?, ?, ?, ?, ?, ?)",
                    sheet, input.grade(), input.pct(), from, to, after);
        }
    }

    /**
     * Withdraws the newest version of the grade's rate for the from-date (null: the one from the start): the version
     * before it, when there is one, is the rate in force again; with none the rate is gone. False when there is no such
     * rate on the sheet.
     */
    public boolean withdraw(String sheet, String grade, LocalDate on) {
        Date from = on == null ? null : Date.valueOf(on);
        List<Integer> newest = jdbc.queryForList("SELECT version FROM rate WHERE sheet = ? AND grade = ?"
                + " AND from_date IS NOT DISTINCT FROM ?", Integer.class, sheet, grade, from);
        if (newest.isEmpty()) {
            return false;
        }
        jdbc.update("DELETE FROM rate WHERE sheet = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?", sheet, grade,
                from);
        List<Integer> older = jdbc.queryForList("SELECT MAX(version) FROM rate_version WHERE sheet = ? AND grade = ?"
                + " AND from_date IS NOT DISTINCT FROM ? AND version < ?", Integer.class, sheet, grade, from,
                newest.get(0));
        Integer previous = older.isEmpty() ? null : older.get(0);
        if (previous != null) {
            jdbc.update("INSERT INTO rate (sheet, grade, pct, from_date, to_date, version, forward_after) SELECT"
                    + " sheet, grade, pct, from_date, to_date, version, forward_after FROM rate_version WHERE sheet = ?"
                    + " AND grade = ? AND from_date IS NOT DISTINCT FROM ? AND version = ?", sheet, grade, from, previous);
            jdbc.update("DELETE FROM rate_version WHERE sheet = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?"
                    + " AND version = ?", sheet, grade, from, previous);
        }
        return true;
    }
}

package net.officefloor.hq.app.rate;

import java.math.RoundingMode;
import java.sql.Date;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.hq.app.setting.Settings;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * The rates kept per sheet: per grade, one for each from-date (and at most one with none), the newest of its versions;
 * the older versions it replaced are kept in rate_version. A sheet is given its
 * parents' rates too, up to {@link Rate#LEVELS} up, each at its level.
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

    /** Every rate on the sheet, its own by grade then from-date (the one with none first), then its parents' by level. */
    public List<Rate> list(String sheet) {
        List<Rate> all = new ArrayList<>(own(sheet));
        String parent = settings.parent(sheet);
        for (int level = 1; level <= Rate.LEVELS && !parent.isEmpty(); level++) {
            for (Rate rate : own(parent)) {
                all.add(rate.up(level));
            }
            parent = settings.parent(parent);
        }
        return all;
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
}

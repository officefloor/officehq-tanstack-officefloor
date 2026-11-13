package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.history.History;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The rates for each grade, kept per sheet as versions (see {@link #set}), each applying from its from-date through its to-date
 *  (if it has one). A rate with no from-date applies from the beginning of time (stored as
 *  {@link #START}). A grade with no rate is absent. When a grade has no rate in force on a date on a
 *  sheet, its parent sheet's (the setting {@code parent}) in force then is used, then the parent's parent,
 *  up to {@link #LEVELS} levels up; then, on a sheet that was cloned, the
 *  rates of the sheet it was cloned from, up its parents and its own source likewise (see {@link #chain}).
 *  <p>A rate saved with a from-date on or after the sheet's today is forward-only: it takes the next
 *  {@code change_seq} as its {@code forward_seq} and applies only to claims added after it (with a later
 *  {@code added_seq}; see {@link #forClaim}). The rate in force (the rates panel, preview and calendar) is read
 *  without regard to this. */
@Repository
public class RateStore {

    /** The stored from-date of a rate that applies from the beginning of time. */
    private static final LocalDate START = LocalDate.of(1, 1, 1);

    /** How many levels of parent sheets up a missing rate is looked for. */
    public static final int LEVELS = 3;

    private final JdbcTemplate jdbc;

    /** Where the rates are read from: the rate table, or the rates as they stood at the end of a date. */
    private final String rates;

    /** Where each sheet's parent is read from: the setting table, or the settings as they stood at the end of a date. */
    private final String settings;

    @Autowired
    public RateStore(JdbcTemplate jdbc) {
        this(jdbc, "rate", "sheet_setting");
    }

    private RateStore(JdbcTemplate jdbc, String rates, String settings) {
        this.jdbc = jdbc;
        this.rates = rates;
        this.settings = settings;
    }

    /** The rates that apply to the claim (to read only): those that are not forward-only, and those saved
     *  before the claim was added. */
    public RateStore forClaim(Claim claim) {
        return new RateStore(jdbc, "(SELECT * FROM " + rates + " WHERE forward_seq IS NULL OR forward_seq < "
                + claim.getAddedSeq() + ") rate", settings);
    }

    /** The rates, and the parents the sheets name, as they stood at the end of the date (see {@link History}), to
     *  read only. A sheet that has no today of its own has kept no dated history (its changes are dated by the
     *  real date), so its rates and its parent are read as they are now. */
    public RateStore asOf(LocalDate date) {
        return new RateStore(jdbc,
                datedOrNow("rate", "sheet_id, grade, from_date", "sheet_id, grade, from_date, to_date, pct, version, forward_seq", date),
                datedOrNow("sheet_setting", "sheet_id, setting_key", "sheet_id, setting_key, setting_value", date));
    }

    /** The table's rows (these columns) as they stood at the end of the date for the sheets with a today of their
     *  own, and as they are now for the others. */
    private static String datedOrNow(String table, String keys, String columns, LocalDate date) {
        String dated = "SELECT id FROM sheet WHERE today IS NOT NULL";
        return "(SELECT " + columns + " FROM " + History.asOf(table, keys, date) + " WHERE sheet_id IN (" + dated + ")"
                + " UNION ALL SELECT " + columns + " FROM " + table + " WHERE sheet_id NOT IN (" + dated + ")) " + table;
    }

    /** The sheet, then its parent, its parent's parent, up to {@link #LEVELS} levels up (stopping at a sheet
     *  with no parent, or one already in the chain); then, when the sheet was cloned, the chain of the sheet it was
     *  cloned from (its source), and so on up the sources. The parents are those named now, or when the rates are
     *  read as of a date, then. */
    public List<String> chain(String sheetId) {
        List<String> chain = new ArrayList<>();
        for (String at = sheetId; at != null && !chain.contains(at); at = source(at)) {
            chain.add(at);
            String bottom = at;
            for (int level = 0; level < LEVELS; level++) {
                List<String> found = jdbc.queryForList("SELECT setting_value FROM " + settings
                        + " WHERE sheet_id = ? AND setting_key = 'parent'", String.class, bottom);
                String parent = found.isEmpty() ? null : found.get(0);
                if (parent == null || chain.contains(parent)) {
                    break;
                }
                chain.add(parent);
                bottom = parent;
            }
        }
        return chain;
    }

    /** The sheet the sheet was cloned from, or null when it was not cloned. */
    private String source(String sheetId) {
        List<String> found = jdbc.queryForList("SELECT source_id FROM sheet_source WHERE sheet_id = ?", String.class, sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Every rate on the sheet, by grade then from-date (level 0); then those of its parent sheets, each
     *  with its level up. */
    public List<RateResponse> list(String sheetId) {
        List<RateResponse> all = new ArrayList<>();
        List<String> chain = chain(sheetId);
        for (int level = 0; level < chain.size(); level++) {
            int at = level;
            all.addAll(jdbc.query("SELECT grade, pct, from_date, to_date, version, forward_seq FROM " + rates + " WHERE sheet_id = ? ORDER BY grade, from_date",
                    (rs, i) -> {
                        LocalDate from = rs.getObject("from_date", LocalDate.class);
                        return RateResponse.of(rs.getString("grade"), rs.getBigDecimal("pct"),
                                START.equals(from) ? null : from, rs.getObject("to_date", LocalDate.class), at, rs.getInt("version"),
                                rs.getObject("forward_seq", Long.class));
                    }, chain.get(level)));
        }
        return all;
    }

    /** The grade's percent in force on the date: the sheet's own (see {@link #own}), else the first of its
     *  parent sheets' (up the chain) that has one; null when none has. */
    public BigDecimal find(String sheetId, String grade, LocalDate on) {
        for (String sheet : chain(sheetId)) {
            BigDecimal pct = own(sheet, grade, on);
            if (pct != null) {
                return pct;
            }
        }
        return null;
    }

    /** The sheet whose rate of the grade is in force on the date (see {@link #find}), or null when none. */
    private String provider(String sheetId, String grade, LocalDate on) {
        for (String sheet : chain(sheetId)) {
            if (own(sheet, grade, on) != null) {
                return sheet;
            }
        }
        return null;
    }

    /** The sheet's own percent for the grade in force on the date (the rate with the latest from-date on or
     *  before it), or null when no rate of the grade applies then, or that rate ended (its to-date) before it. */
    private BigDecimal own(String sheetId, String grade, LocalDate on) {
        List<BigDecimal> found = jdbc.query(
                "SELECT pct, to_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1",
                (rs, i) -> {
                    LocalDate to = rs.getObject("to_date", LocalDate.class);
                    return to != null && to.isBefore(on) ? null : rs.getBigDecimal("pct");
                }, sheetId, grade, on);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Whether no rate of the grade is in force on the date on the sheet or its parents, and the one that would
     *  apply on one of them (the one with the latest from-date on or before it) ended (its to-date) before it. */
    public boolean ended(String sheetId, String grade, LocalDate on) {
        if (find(sheetId, grade, on) != null) {
            return false;
        }
        return chain(sheetId).stream().anyMatch(sheet -> ownEnded(sheet, grade, on));
    }

    private boolean ownEnded(String sheetId, String grade, LocalDate on) {
        List<LocalDate> found = jdbc.query(
                "SELECT to_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1",
                (rs, i) -> rs.getObject("to_date", LocalDate.class), sheetId, grade, on);
        return !found.isEmpty() && found.get(0) != null && found.get(0).isBefore(on);
    }

    /** Whether the grade's rate in force on the date ends before {@code today}: its to-date, or the day
     *  before the grade's next rate starts, whichever is first, is before it. The rate is that of the sheet
     *  that provides it (see {@link #find}); a rate starting on a sheet nearer down the chain ends it too. */
    public boolean endsBefore(String sheetId, String grade, LocalDate on, LocalDate today) {
        String provider = provider(sheetId, grade, on);
        if (provider == null) {
            return false;
        }
        for (String sheet : chain(sheetId)) {
            if (sheet.equals(provider)) {
                return ownEndsBefore(sheet, grade, on, today);
            }
            List<LocalDate> next = jdbc.queryForList(
                    "SELECT from_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date > ? ORDER BY from_date LIMIT 1",
                    LocalDate.class, sheet, grade, on);
            if (!next.isEmpty() && !next.get(0).isAfter(today)) {
                return true;
            }
        }
        return false;
    }

    private boolean ownEndsBefore(String sheetId, String grade, LocalDate on, LocalDate today) {
        List<LocalDate> to = jdbc.query(
                "SELECT to_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1",
                (rs, i) -> rs.getObject("to_date", LocalDate.class), sheetId, grade, on);
        if (to.isEmpty()) {
            return false;
        }
        if (to.get(0) != null && to.get(0).isBefore(today)) {
            return true;
        }
        List<LocalDate> next = jdbc.queryForList(
                "SELECT from_date FROM " + rates + " WHERE sheet_id = ? AND grade = ? AND from_date > ? ORDER BY from_date LIMIT 1",
                LocalDate.class, sheetId, grade, on);
        return !next.isEmpty() && !next.get(0).isAfter(today);
    }

    /** Sets the grade's rate from the date (null for from the beginning of time) through the to-date
     *  (null for no end): forward-only when the from-date is on or after the sheet's {@code today}. When the grade and from-date already have a rate, the value it replaces is kept as an
     *  older version and this is added as the newest, the one in force. */
    public void set(String sheetId, String grade, LocalDate from, LocalDate to, BigDecimal pct, LocalDate today) {
        LocalDate at = from == null ? START : from;
        int version = version(sheetId, grade, from) + 1;
        Long forward = from != null && !from.isBefore(today)
                ? jdbc.queryForObject("SELECT NEXT VALUE FOR change_seq", Long.class)
                : null;
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, pct, to_date, forward_seq) VALUES (?, ?, ?, ?, ?, ?, ?)",
                sheetId, grade, at, version, pct, to, forward);
        jdbc.update("MERGE INTO rate (sheet_id, grade, from_date, to_date, pct, version, forward_seq) KEY (sheet_id, grade, from_date) VALUES (?, ?, ?, ?, ?, ?, ?)",
                sheetId, grade, at, to, pct, version, forward);
    }

    /** Withdraws the newest version of the grade's rate from the date (null for from the beginning of time): the
     *  version before it, when there is one, is in force again; with none, the rate is gone. */
    public void withdraw(String sheetId, String grade, LocalDate from) {
        LocalDate at = from == null ? START : from;
        int version = version(sheetId, grade, from);
        jdbc.update("DELETE FROM rate_version WHERE sheet_id = ? AND grade = ? AND from_date = ? AND version = ?",
                sheetId, grade, at, version);
        List<Object[]> before = jdbc.query("SELECT pct, to_date, forward_seq FROM rate_version WHERE sheet_id = ? AND grade = ?"
                + " AND from_date = ? AND version = ?",
                (rs, i) -> new Object[] {rs.getBigDecimal("pct"), rs.getObject("to_date", LocalDate.class), rs.getObject("forward_seq", Long.class)},
                sheetId, grade, at, version - 1);
        if (before.isEmpty()) {
            jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ? AND from_date = ?", sheetId, grade, at);
        } else {
            Object[] v = before.get(0);
            jdbc.update("UPDATE rate SET pct = ?, to_date = ?, version = ?, forward_seq = ? WHERE sheet_id = ? AND grade = ? AND from_date = ?",
                    v[0], v[1], version - 1, v[2], sheetId, grade, at);
        }
    }

    /** When the sheet's rate of the grade from the date (null for from the beginning of time) was saved, when it
     *  is forward-only; else null. */
    public Long forward(String sheetId, String grade, LocalDate from) {
        List<Long> found = jdbc.queryForList("SELECT forward_seq FROM rate WHERE sheet_id = ? AND grade = ? AND from_date = ?",
                Long.class, sheetId, grade, from == null ? START : from);
        return found.isEmpty() ? null : found.get(0);
    }

    /** How many versions the sheet's rate of the grade from the date (null for from the beginning of time)
     *  has; 0 when it has none. */
    public int version(String sheetId, String grade, LocalDate from) {
        Integer found = jdbc.queryForObject("SELECT MAX(version) FROM rate_version WHERE sheet_id = ? AND grade = ? AND from_date = ?",
                Integer.class, sheetId, grade, from == null ? START : from);
        return found == null ? 0 : found;
    }
}

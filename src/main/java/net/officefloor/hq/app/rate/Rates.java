package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** The rate store: percents per grade, each from a date (or from the beginning of time), kept per sheet. */
@Repository
public class Rates {

    private final JdbcTemplate jdbc;

    private final Sheets sheets;

    public Rates(JdbcTemplate jdbc, Sheets sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's rates, by grade then from-date (from the beginning of time first), each with the older versions it
     *  replaced (newest first) for the claims a forward-only newer version does not apply to. */
    public List<RateResponse> list(String sheetId) {
        Map<String, List<RateResponse.Earlier>> earlier = new HashMap<>();
        jdbc.query("SELECT grade, from_date, rate, to_date, after_mark FROM rate_version WHERE sheet_id = ? ORDER BY version DESC",
                (RowCallbackHandler) rs -> {
                    Date to = rs.getDate("to_date");
                    earlier.computeIfAbsent(rs.getString("grade") + "|" + rs.getDate("from_date"), k -> new ArrayList<>())
                            .add(new RateResponse.Earlier(rs.getBigDecimal("rate").setScale(2).toPlainString(),
                                    to == null ? "" : to.toLocalDate().toString(), rs.getLong("after_mark")));
                }, sheetId);
        return jdbc.query("SELECT grade, rate, from_date, to_date, version, after_mark FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST",
                (rs, i) -> {
                    Date from = rs.getDate("from_date");
                    Date to = rs.getDate("to_date");
                    return new RateResponse(rs.getString("grade"), rs.getBigDecimal("rate").setScale(2).toPlainString(),
                            from == null ? "" : from.toLocalDate().toString(), to == null ? "" : to.toLocalDate().toString(),
                            rs.getInt("version"), rs.getLong("after_mark"),
                            earlier.getOrDefault(rs.getString("grade") + "|" + from, List.of()));
                },
                sheetId);
    }

    /** A claim's mark later than every rate's: asks for the rate in force without regard to forward-only rates (the
     *  rates panel, the rate preview and the rate calendar show that one). */
    public static final long IN_FORCE = Long.MAX_VALUE;

    /** Whether a rate (version) saved with this mark applies to a claim with that one: a rate applying to every claim
     *  (mark 0) always; a forward-only one only to a claim that arrived after it was saved. */
    public static boolean applies(long afterMark, long claimMark) {
        return afterMark == 0 || claimMark > afterMark;
    }

    /** The rate for a grade in force on a date for a claim with a mark (see {@link #applies}). */
    @FunctionalInterface
    public interface Lookup {
        BigDecimal rate(String grade, LocalDate date, long claimMark);
    }

    /** How many levels up a sheet's parents are asked for a rate the sheet has not. */
    public static final int ANCESTOR_LEVELS = 3;

    /** The sheet's ancestors asked for a rate it has not: its parent, then the parent's parent, up to
     *  {@link #ANCESTOR_LEVELS} levels up; then, for a clone, the sheet it was cloned from and that sheet's own
     *  ancestors, and so on. */
    public List<String> ancestors(String sheetId) {
        return chain(sheetId, sheets::parent, sheets::source);
    }

    /** The chain of sheets asked for a rate the sheet has not, by the parent and source of each: the sheet's parents up to
     *  {@link #ANCESTOR_LEVELS} levels up, then its source (when it is a clone) and that one's parents and source in turn;
     *  each sheet once. */
    public static List<String> chain(String sheetId, java.util.function.UnaryOperator<String> parent,
            java.util.function.UnaryOperator<String> source) {
        List<String> chain = new ArrayList<>();
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        for (String from = sheetId; ; ) {
            int levels = 0;
            for (String at = parent.apply(from); at != null && levels < ANCESTOR_LEVELS && seen.add(at); at = parent.apply(at)) {
                chain.add(at);
                levels++;
            }
            String next = source.apply(from);
            if (next == null || !seen.add(next)) {
                return chain;
            }
            chain.add(next);
            from = next;
        }
    }

    /** The rates of the sheet's ancestors (see {@link #ancestors(String)}), nearest first. */
    public List<List<RateResponse>> inherited(String sheetId) {
        return ancestors(sheetId).stream().map(this::list).toList();
    }

    /** The rate for a grade on the sheet in force on a date (see {@link #findOwn}); when the sheet has
     *  none, its parent's, then the parent's parent's, up to {@link #ANCESTOR_LEVELS} levels up, then a clone's source's
     *  (see {@link #ancestors}); null when none has one. */
    public BigDecimal find(String sheetId, String grade, LocalDate date) {
        return find(sheetId, grade, date, IN_FORCE);
    }

    /** The rate for a grade on the sheet that applies to the claim on its date (see {@link #find(String, String, LocalDate, long)}). */
    public BigDecimal find(net.officefloor.hq.app.claim.Claim claim, String grade) {
        return find(claim.getSheetId(), grade, claim.getDate(), claim.getRateMark());
    }

    /** As {@link #find(String, String, LocalDate)}, for a claim with a mark: the rates that apply to it only. */
    public BigDecimal find(String sheetId, String grade, LocalDate date, long claimMark) {
        BigDecimal own = findOwn(sheetId, grade, date, claimMark);
        if (own != null) {
            return own;
        }
        for (String ancestor : ancestors(sheetId)) {
            BigDecimal rate = findOwn(ancestor, grade, date, claimMark);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /** The rate for a grade set on the sheet itself in force on a date for a claim with a mark: of the versions that apply
     *  to it (see {@link #applies}), the newest of the one with the latest from-date on or before the date; null when none
     *  applies, or when that one's to-date is before the date. */
    public BigDecimal findOwn(String sheetId, String grade, LocalDate date, long claimMark) {
        Date on = Date.valueOf(date);
        String cols = "SELECT from_date, version, rate, to_date, after_mark FROM ";
        String where = " WHERE sheet_id = ? AND grade = ? AND (from_date IS NULL OR from_date <= ?)";
        List<Object[]> found = jdbc.query(cols + "rate" + where + " UNION ALL " + cols + "rate_version" + where
                + " ORDER BY from_date DESC NULLS LAST, version DESC",
                (rs, i) -> new Object[] { rs.getBigDecimal("rate"), rs.getDate("to_date"), rs.getLong("after_mark") },
                sheetId, grade, on, sheetId, grade, on);
        for (Object[] row : found) {
            if (applies((Long) row[2], claimMark)) {
                Date to = (Date) row[1];
                return to != null && to.before(on) ? null : (BigDecimal) row[0];
            }
        }
        return null;
    }

    /** The due on an amount at a rate (a percent), rounded to cents by the claim's rounding; null when there is no rate. */
    public static BigDecimal due(BigDecimal amount, BigDecimal rate, RoundingMode rounding) {
        return rate == null ? null : amount.multiply(rate).movePointLeft(2).setScale(2, rounding);
    }

    /** Set the grade's rate from a date (null: from the beginning of time) through a date (null: no
     *  end) as a new version: one already there from the same date is kept as an older version. One from a date on or
     *  after the sheet's today is forward-only: it draws a mark, and applies only to claims arriving after it. */
    public void set(String sheetId, String grade, LocalDate from, LocalDate to, BigDecimal rate) {
        Date fromDate = from == null ? null : Date.valueOf(from);
        long afterMark = from != null && !from.isBefore(sheets.today(sheetId))
                ? jdbc.queryForObject("SELECT NEXT VALUE FOR rate_mark_seq", Long.class) : 0L;
        String sameFrom = fromDate == null ? " AND from_date IS NULL" : " AND from_date = ?";
        Object[] key = fromDate == null ? new Object[] { sheetId, grade } : new Object[] { sheetId, grade, fromDate };
        jdbc.update("INSERT INTO rate_version (sheet_id, grade, from_date, version, rate, to_date, after_mark)"
                + " SELECT sheet_id, grade, from_date, version, rate, to_date, after_mark FROM rate WHERE sheet_id = ? AND grade = ?" + sameFrom, key);
        Integer latest = jdbc.queryForObject("SELECT MAX(version) FROM rate WHERE sheet_id = ? AND grade = ?" + sameFrom, Integer.class, key);
        jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ?" + sameFrom, key);
        jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date, version, after_mark) VALUES (?, ?, ?, ?, ?, ?, ?)",
                sheetId, grade, rate, fromDate, to == null ? null : Date.valueOf(to), latest == null ? 1 : latest + 1, afterMark);
        jdbc.update("INSERT INTO rate_history (sheet_id, set_on, grade, rate, from_date, to_date, after_mark)"
                + " VALUES (?, (SELECT today FROM sheet WHERE id = ?), ?, ?, ?, ?, ?)",
                sheetId, sheetId, grade, rate, fromDate, to == null ? null : Date.valueOf(to), afterMark);
    }

    /** Withdraw the newest version of the grade's rate from a date (null: from the beginning of time): the version
     *  before it, when there is one, is the rate again; with none the rate is gone. False when there is no such rate. */
    public boolean withdraw(String sheetId, String grade, LocalDate from) {
        Date fromDate = from == null ? null : Date.valueOf(from);
        String sameFrom = fromDate == null ? " AND from_date IS NULL" : " AND from_date = ?";
        Object[] key = fromDate == null ? new Object[] { sheetId, grade } : new Object[] { sheetId, grade, fromDate };
        if (jdbc.update("DELETE FROM rate WHERE sheet_id = ? AND grade = ?" + sameFrom, key) == 0) {
            return false;
        }
        Integer previous = jdbc.queryForObject("SELECT MAX(version) FROM rate_version WHERE sheet_id = ? AND grade = ?" + sameFrom,
                Integer.class, key);
        if (previous != null) {
            Object[] versionKey = java.util.stream.Stream.concat(java.util.Arrays.stream(key), java.util.stream.Stream.of(previous)).toArray();
            jdbc.update("INSERT INTO rate (sheet_id, grade, rate, from_date, to_date, version, after_mark)"
                    + " SELECT sheet_id, grade, rate, from_date, to_date, version, after_mark FROM rate_version"
                    + " WHERE sheet_id = ? AND grade = ?" + sameFrom + " AND version = ?", versionKey);
            jdbc.update("DELETE FROM rate_version WHERE sheet_id = ? AND grade = ?" + sameFrom + " AND version = ?", versionKey);
        }
        return true;
    }
}

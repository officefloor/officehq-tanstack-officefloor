package net.officefloor.hq.app.rate;

import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored rates, one per grade and from-date per sheet: the newest version, the one in force, with the older versions
 *  it replaced kept in rate_version. A grade with no row has no rate. */
@Repository
public class RateRepository {

    private static final String COLUMNS = "sheet_id, grade, rate, from_date, to_date, after_seq";
    private static final String VERSIONED = COLUMNS + ", version";

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;
    private final SettingRepository settings;

    public RateRepository(JdbcTemplate jdbc, SheetRepository sheets, SettingRepository settings) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.settings = settings;
    }

    /** The sheets whose rates the sheet uses, in the order looked at: itself, then its parent, the parent's parent,
     *  up to SettingRepository#PARENT_LEVELS levels up. */
    public List<String> chain(String sheetId) {
        List<String> chain = new ArrayList<>();
        chain.add(sheetId);
        chain.addAll(settings.ancestors(sheetId, SettingRepository.PARENT_LEVELS));
        return chain;
    }

    /** As chain, as of a date: the parent the sheet had then (see SettingRepository#parentAsOf), none when it had
     *  none, then that parent's ancestors as they are now. */
    public List<String> chainAsOf(String sheetId, LocalDate asOf) {
        List<String> chain = new ArrayList<>();
        chain.add(sheetId);
        String parent = settings.parentAsOf(sheetId, asOf);
        if (parent != null && !parent.equals(sheetId)) {
            chain.add(parent);
            for (String up : settings.ancestors(parent, SettingRepository.PARENT_LEVELS - 1)) {
                if (up.equals(sheetId) || chain.contains(up)) {
                    break;
                }
                chain.add(up);
            }
        }
        return chain;
    }

    /** The sheet's rates, by grade, then from-date (one with none first). */
    public List<Rate> list(String sheetId) {
        return jdbc.query("SELECT " + VERSIONED + " FROM rate WHERE sheet_id = ? ORDER BY grade, from_date NULLS FIRST",
                RateRepository::map, sheetId);
    }

    /** The grade's rate in force on a date on the sheet (see own), or, when it has none, the one in force on its
     *  parent, then the parent's parent, up to three levels up; null when none of them has one. */
    public Rate find(String sheetId, String grade, LocalDate on) {
        for (String sheet : chain(sheetId)) {
            Rate rate = own(sheet, grade, on);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /** The grade's rate in force on a date — the latest from-date on or before it, a rate with none applying
     *  from the beginning of time — or null when no rate applies, or that rate's to-date is before the date. */
    public Rate own(String sheetId, String grade, LocalDate on) {
        List<Rate> found = jdbc.query("SELECT " + VERSIONED + " FROM rate WHERE sheet_id = ? AND grade = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1",
                RateRepository::map, sheetId, grade, Date.valueOf(on));
        if (found.isEmpty() || (found.get(0).to() != null && found.get(0).to().isBefore(on))) {
            return null;
        }
        return found.get(0);
    }

    /** The grade's rate that applies to a claim — dated {@code on}, added with the added sequence {@code seq} (null: one
     *  not yet added) — on the sheet (see ownFor), or, when it has none, on its parent, then the parent's parent, up to
     *  three levels up; null when none of them has one. Unlike find, a forward-only rate saved after the claim was added
     *  is passed over (see Rate#appliesTo). */
    public Rate forClaim(String sheetId, String grade, LocalDate on, Long seq) {
        for (String sheet : chain(sheetId)) {
            Rate rate = ownFor(sheet, grade, on, seq);
            if (rate != null) {
                return rate;
            }
        }
        return null;
    }

    /** The grade's rate on the sheet that applies to a claim dated {@code on} added with the added sequence {@code seq}:
     *  see #pick, over every version of the grade's rates. */
    public Rate ownFor(String sheetId, String grade, LocalDate on, Long seq) {
        List<Rate> all = new ArrayList<>(jdbc.query("SELECT " + VERSIONED + " FROM rate WHERE sheet_id = ? AND grade = ?"
                + " AND (from_date IS NULL OR from_date <= ?)", RateRepository::map, sheetId, grade, Date.valueOf(on)));
        if (all.stream().anyMatch((r) -> r.after() != null) || hasForward(sheetId, grade)) {
            all.addAll(jdbc.query("SELECT " + VERSIONED + " FROM rate_version WHERE sheet_id = ? AND grade = ?"
                    + " AND (from_date IS NULL OR from_date <= ?)", RateRepository::map, sheetId, grade, Date.valueOf(on)));
        }
        return pick(all, grade, on, seq);
    }

    private boolean hasForward(String sheetId, String grade) {
        Integer n = jdbc.queryForObject("SELECT COUNT(*) FROM rate_version WHERE sheet_id = ? AND grade = ?"
                + " AND after_seq IS NOT NULL", Integer.class, sheetId, grade);
        return n != null && n > 0;
    }

    /** Of the versions given (of one sheet's rates), the grade's rate that applies to a claim dated {@code on} added
     *  with the added sequence {@code seq} (null: one not yet added): for each from-date, the newest version that
     *  applies to the claim (see Rate#appliesTo), then of those the one with the latest from-date on or before the
     *  date, one with none applying from the beginning of time; null when none applies, or that rate's to-date is
     *  before the date. */
    public static Rate pick(List<Rate> versions, String grade, LocalDate on, Long seq) {
        Rate found = null;
        for (Rate r : versions) {
            if (!r.grade().equals(grade) || (r.from() != null && r.from().isAfter(on)) || !r.appliesTo(seq)) {
                continue;
            }
            int byFrom = found == null ? 1
                    : r.from() == null ? (found.from() == null ? 0 : -1)
                    : found.from() == null ? 1 : r.from().compareTo(found.from());
            if (byFrom > 0 || (byFrom == 0 && r.version() > found.version())) {
                found = r;
            }
        }
        return found == null || (found.to() != null && found.to().isBefore(on)) ? null : found;
    }

    /** The older versions of the grade's rate for a from-date on the sheet, newest first. */
    public List<Rate> older(String sheetId, String grade, LocalDate from) {
        return jdbc.query("SELECT " + VERSIONED + " FROM rate_version WHERE sheet_id = ? AND grade = ?"
                + " AND from_date IS NOT DISTINCT FROM ? ORDER BY version DESC", RateRepository::map, sheetId, grade,
                from == null ? null : Date.valueOf(from));
    }

    /** The added sequence of a claim (see Rate#appliesTo); null when there is no such claim. */
    public Long seqOf(long claimId) {
        List<Long> found = jdbc.queryForList("SELECT added_seq FROM claim WHERE id = ?", Long.class, claimId);
        return found.isEmpty() ? null : found.get(0);
    }

    /** Every rate stored on the sheet while its today was on or before the date, in the order stored. */
    public List<Rate> storedBy(String sheetId, LocalDate madeOn) {
        return jdbc.query("SELECT " + COLUMNS + " FROM rate_log WHERE sheet_id = ? AND made_on <= ? ORDER BY id",
                RateRepository::logged, sheetId, Date.valueOf(madeOn));
    }

    /** Stores the rate as the newest version of the grade's rate for its from-date: the one it had for that from-date
     *  is kept as an older version, the new one numbered after it (1 when it had none). One with a from-date on or after
     *  the sheet's today is forward-only: it applies only to claims added after now (see Rate#after). Logs it with the
     *  sheet's today. */
    public Rate save(Rate set) {
        LocalDate today = sheets.today(set.sheetId());
        if (set.from() != null && !set.from().isBefore(today)) {
            set = set.withAfter(jdbc.queryForObject("SELECT COALESCE(MAX(added_seq), 0) FROM claim", Long.class));
        }
        Date from = set.from() == null ? null : Date.valueOf(set.from());
        String same = " WHERE sheet_id = ? AND grade = ? AND from_date IS NOT DISTINCT FROM ?";
        jdbc.update("INSERT INTO rate_version (" + VERSIONED + ") SELECT " + VERSIONED + " FROM rate" + same,
                set.sheetId(), set.grade(), from);
        Integer last = jdbc.queryForObject("SELECT MAX(version) FROM rate" + same, Integer.class,
                set.sheetId(), set.grade(), from);
        Rate rate = set.withVersion(last == null ? 1 : last + 1);
        jdbc.update("DELETE FROM rate" + same, rate.sheetId(), rate.grade(), from);
        jdbc.update("INSERT INTO rate (" + VERSIONED + ") VALUES (?, ?, ?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate(), from, rate.to() == null ? null : Date.valueOf(rate.to()),
                rate.after(), rate.version());
        jdbc.update("INSERT INTO rate_log (" + COLUMNS + ", made_on) VALUES (?, ?, ?, ?, ?, ?, ?)",
                rate.sheetId(), rate.grade(), rate.rate(), from, rate.to() == null ? null : Date.valueOf(rate.to()),
                rate.after(), Date.valueOf(today));
        return rate;
    }

    private static Rate map(ResultSet rs, int n) throws SQLException {
        return logged(rs, n).withVersion(rs.getInt("version"));
    }

    /** A rate as logged, which keeps no version (see SheetAsOf for how it counts them). */
    private static Rate logged(ResultSet rs, int n) throws SQLException {
        Date from = rs.getDate("from_date");
        Date to = rs.getDate("to_date");
        long after = rs.getLong("after_seq");
        return new Rate(rs.getString("sheet_id"), rs.getString("grade"), rs.getBigDecimal("rate").setScale(2),
                from == null ? null : from.toLocalDate(), to == null ? null : to.toLocalDate())
                .withAfter(rs.wasNull() ? null : after);
    }
}

package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The settings kept per sheet, by key; a key with no value set takes its default. A setting that carries a figure is
 * {@link #dated}: it keeps a value per from-date, the one in force on a date being that of the latest from-date on or
 * before it; the others keep one value.
 */
@Repository
public class Settings {

    /** The due a clerk may settle at most (two decimals); blank: no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The most a person's due-total is (two decimals); blank: no cap. */
    public static final String CAP = "cap";

    /** The due above which a claim needs a proof before it is settled or approved (two decimals); blank: none. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** The least a claim's live due is (two decimals); blank: its default. */
    public static final String FLOOR = "floor";

    /** The days after its date by which a claim is due (a whole number); blank: its default. */
    public static final String TERMS = "terms";

    /** The grades that may have terms of their own, each under the key terms-<grade>. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    /** The key of a grade's own terms (a whole number of days); blank: the sheet's terms. */
    public static String gradeTerms(String grade) {
        return TERMS + "-" + grade;
    }

    /** The percent of an overdue claim's figure charged for every full 30 days it is overdue (two decimals). */
    public static final String LATE_PCT = "late-pct";

    /** The date before which a claim rounds half up, on or after it half to even (yyyy-MM-dd); blank: the sheet's today. */
    public static final String POLICY_DATE = "policy-date";

    /** The date from which (on the sheet's today) a claim opened is numbered within its period (yyyy-MM-dd); blank: off. */
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The late percent a sheet starts with. */
    public static final String DEFAULT_LATE_PCT = "0.00";

    /** The terms a sheet starts with. */
    public static final String DEFAULT_TERMS = "14";

    /** The floor a sheet starts with. */
    public static final String DEFAULT_FLOOR = "1.00";

    /** Whether the key is dated: every setting that carries a figure is; the policy and period numbering dates are not. */
    public static boolean dated(String key) {
        return !POLICY_DATE.equals(key) && !PERIOD_NUMBERING_FROM.equals(key);
    }

    /** The from-date an undated setting's one value is kept under. */
    private static final Date UNDATED = Date.valueOf(LocalDate.of(1, 1, 1));

    private final JdbcTemplate jdbc;

    private final SheetClock clock;

    public Settings(JdbcTemplate jdbc, SheetClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** The value set for an undated key, or "" when none is. */
    public String get(String sheet, String key) {
        List<String> found = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet = ? AND setting_key = ?",
                String.class, sheet, key);
        return found.isEmpty() ? "" : found.get(0);
    }

    /** The value of a dated key in force on the date, or "" when none is recorded on or before it. */
    public String on(String sheet, String key, LocalDate date) {
        List<String> found = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet = ? AND setting_key = ?"
                + " AND from_date <= ? ORDER BY from_date DESC LIMIT 1", String.class, sheet, key, Date.valueOf(date));
        return found.isEmpty() ? "" : found.get(0);
    }

    /** The values recorded for a dated key, by from-date. */
    public List<DatedValue> history(String sheet, String key) {
        return jdbc.query("SELECT from_date, setting_value FROM setting WHERE sheet = ? AND setting_key = ? ORDER BY from_date",
                (rs, i) -> new DatedValue(rs.getDate(1).toLocalDate().toString(), rs.getString(2)), sheet, key);
    }

    /** Sets an undated key's value. */
    public void set(String sheet, String key, String value) {
        set(sheet, key, null, value);
    }

    /** Records the key's value from the from-date (an undated key ignores it), replacing one already recorded from it. */
    public void set(String sheet, String key, LocalDate from, String value) {
        Date date = dated(key) && from != null ? Date.valueOf(from) : UNDATED;
        jdbc.update("MERGE INTO setting (sheet, setting_key, from_date, setting_value) KEY (sheet, setting_key, from_date)"
                + " VALUES (?, ?, ?, ?)", sheet, key, date, value);
    }

    /** The dated key's figure by date, starting (before every from-date, or where a blank is in force) at the start. */
    private Dated figure(String sheet, String key, BigDecimal start) {
        TreeMap<LocalDate, BigDecimal> values = new TreeMap<>();
        for (DatedValue v : history(sheet, key)) {
            values.put(LocalDate.parse(v.from()), v.value().isEmpty() ? start : new BigDecimal(v.value()));
        }
        return new Dated(start, values);
    }

    /** The sheet's clerk limit in force on its today, or null when there is none. */
    public BigDecimal clerkLimit(String sheet) {
        String limit = on(sheet, CLERK_LIMIT, clock.today(sheet));
        return limit.isEmpty() ? null : new BigDecimal(limit);
    }

    /** The sheet's cap in force on its today: the most a person's due-total is, or null when there is none. */
    public BigDecimal cap(String sheet) {
        String cap = on(sheet, CAP, clock.today(sheet));
        return cap.isEmpty() ? null : new BigDecimal(cap);
    }

    /** The due-total capped at the cap (none: as it is); a null (n/a) due-total is not capped. */
    public static BigDecimal capped(BigDecimal dueTotal, BigDecimal cap) {
        return dueTotal == null || cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }

    /** The sheet's proof threshold in force on the date, or null when there is none. */
    public BigDecimal proofThreshold(String sheet, LocalDate date) {
        String threshold = on(sheet, PROOF_THRESHOLD, date);
        return threshold.isEmpty() ? null : new BigDecimal(threshold);
    }

    /** The sheet's floor by date: the least a claim's live due is, the one in force on the claim's date. */
    public Dated floor(String sheet) {
        return figure(sheet, FLOOR, new BigDecimal(DEFAULT_FLOOR));
    }

    /** The sheet's terms in force on the date: the days after its date by which a claim is due. */
    public int terms(String sheet, LocalDate date) {
        String terms = on(sheet, TERMS, date);
        return Integer.parseInt(terms.isEmpty() ? DEFAULT_TERMS : terms);
    }

    /**
     * The terms of the grade in force on the date: its own when it has them then, else the sheet's (a null grade takes
     * the sheet's).
     */
    public int terms(String sheet, String grade, LocalDate date) {
        String own = grade == null ? "" : on(sheet, gradeTerms(grade), date);
        return own.isEmpty() ? terms(sheet, date) : Integer.parseInt(own);
    }

    /**
     * The sheet's late percent by date: charged on an overdue claim's figure for every full 30 days it is overdue, the
     * one in force on the claim's date.
     */
    public Dated latePct(String sheet) {
        return figure(sheet, LATE_PCT, new BigDecimal(DEFAULT_LATE_PCT));
    }

    /** What makes the sheet's late charges on its today. */
    public Late late(String sheet, LocalDate today) {
        String policy = get(sheet, POLICY_DATE);
        return new Late(latePct(sheet), today, policy.isEmpty() ? today : LocalDate.parse(policy));
    }

    /** The date from which claims opened are numbered within their period, or null when they are not. */
    public LocalDate periodNumberingFrom(String sheet) {
        String from = get(sheet, PERIOD_NUMBERING_FROM);
        return from.isEmpty() ? null : LocalDate.parse(from);
    }

    /** The settings in force on the sheet's today, with every dated key's recorded values. */
    public SettingsResponse all(String sheet) {
        LocalDate today = clock.today(sheet);
        Map<String, List<DatedValue>> dated = new LinkedHashMap<>();
        for (String key : List.of(CLERK_LIMIT, CAP, FLOOR, TERMS, gradeTerms("A"), gradeTerms("B"), gradeTerms("C"), LATE_PCT, PROOF_THRESHOLD)) {
            dated.put(key, history(sheet, key));
        }
        return new SettingsResponse(on(sheet, CLERK_LIMIT, today), floor(sheet).on(today).toPlainString(),
                Integer.toString(terms(sheet, today)), latePct(sheet).on(today).toPlainString(), get(sheet, POLICY_DATE),
                get(sheet, PERIOD_NUMBERING_FROM), on(sheet, gradeTerms("A"), today), on(sheet, gradeTerms("B"), today),
                on(sheet, gradeTerms("C"), today), on(sheet, CAP, today), on(sheet, PROOF_THRESHOLD, today), dated);
    }
}

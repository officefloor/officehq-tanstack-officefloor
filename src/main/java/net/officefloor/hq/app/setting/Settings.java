package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Late;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings kept per sheet, by key; a key with no value set takes its default. */
@Repository
public class Settings {

    /** The due a clerk may settle at most (two decimals); blank: no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The most a person's due-total is (two decimals); blank: no cap. */
    public static final String CAP = "cap";

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

    private final JdbcTemplate jdbc;

    public Settings(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The value set for the key, or "" when none is. */
    public String get(String sheet, String key) {
        List<String> found = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet = ? AND setting_key = ?",
                String.class, sheet, key);
        return found.isEmpty() ? "" : found.get(0);
    }

    public void set(String sheet, String key, String value) {
        jdbc.update("MERGE INTO setting (sheet, setting_key, setting_value) KEY (sheet, setting_key) VALUES (?, ?, ?)", sheet,
                key, value);
    }

    /** The sheet's clerk limit, or null when there is none. */
    public BigDecimal clerkLimit(String sheet) {
        String limit = get(sheet, CLERK_LIMIT);
        return limit.isEmpty() ? null : new BigDecimal(limit);
    }

    /** The sheet's cap: the most a person's due-total is, or null when there is none. */
    public BigDecimal cap(String sheet) {
        String cap = get(sheet, CAP);
        return cap.isEmpty() ? null : new BigDecimal(cap);
    }

    /** The due-total capped at the cap (none: as it is); a null (n/a) due-total is not capped. */
    public static BigDecimal capped(BigDecimal dueTotal, BigDecimal cap) {
        return dueTotal == null || cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }

    /** The sheet's floor: the least a claim's live due is. */
    public BigDecimal floor(String sheet) {
        String floor = get(sheet, FLOOR);
        return new BigDecimal(floor.isEmpty() ? DEFAULT_FLOOR : floor);
    }

    /** The sheet's terms: the days after its date by which a claim is due. */
    public int terms(String sheet) {
        String terms = get(sheet, TERMS);
        return Integer.parseInt(terms.isEmpty() ? DEFAULT_TERMS : terms);
    }

    /** The terms of the grade: its own when it has them, else the sheet's (a null grade takes the sheet's). */
    public int terms(String sheet, String grade) {
        String own = grade == null ? "" : get(sheet, gradeTerms(grade));
        return own.isEmpty() ? terms(sheet) : Integer.parseInt(own);
    }

    /** The sheet's late percent: charged on an overdue claim's figure for every full 30 days it is overdue. */
    public BigDecimal latePct(String sheet) {
        String pct = get(sheet, LATE_PCT);
        return new BigDecimal(pct.isEmpty() ? DEFAULT_LATE_PCT : pct);
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

    public SettingsResponse all(String sheet) {
        return new SettingsResponse(get(sheet, CLERK_LIMIT), floor(sheet).toPlainString(),
                Integer.toString(terms(sheet)), latePct(sheet).toPlainString(), get(sheet, POLICY_DATE),
                get(sheet, PERIOD_NUMBERING_FROM), get(sheet, gradeTerms("A")), get(sheet, gradeTerms("B")),
                get(sheet, gradeTerms("C")), get(sheet, CAP));
    }
}

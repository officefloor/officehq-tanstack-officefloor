package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings kept per sheet, by key. A setting never set, or set blank, has no value. */
@Repository
public class SettingStore {

    public static final String CLERK_LIMIT = "clerk-limit";

    public static final String FLOOR = "floor";

    public static final String CAP = "cap";

    public static final String TERMS = "terms";

    /** The key of a grade's own terms: terms-A, terms-B or terms-C. */
    public static final String TERMS_PREFIX = "terms-";

    /** The grades that may have terms of their own. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    public static final String LATE_PCT = "late-pct";

    public static final String POLICY_DATE = "policy-date";

    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The late percent a sheet has until one is set. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The terms (days) a sheet has until they are set. */
    public static final int DEFAULT_TERMS = 14;

    /** The floor a sheet has until one is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    private final JdbcTemplate jdbc;

    public SettingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The setting's value, or null when it has none. */
    public String find(String sheetId, String key) {
        List<String> found = jdbc.queryForList(
                "SELECT setting_value FROM sheet_setting WHERE sheet_id = ? AND setting_key = ?", String.class,
                sheetId, key);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The most a clerk may settle a claim's due at, or null for no limit. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = find(sheetId, CLERK_LIMIT);
        return value == null ? null : new BigDecimal(value);
    }

    /** The most a person's due-total may be, or null for no cap. */
    public BigDecimal cap(String sheetId) {
        String value = find(sheetId, CAP);
        return value == null ? null : new BigDecimal(value);
    }

    /** The due-total at most the cap (none when null). */
    public static BigDecimal capped(BigDecimal dueTotal, BigDecimal cap) {
        return cap != null && dueTotal.compareTo(cap) > 0 ? cap.setScale(2) : dueTotal;
    }

    /** The least a claim's live due may be: the floor set, else 1.00. */
    public BigDecimal floor(String sheetId) {
        String value = find(sheetId, FLOOR);
        return value == null ? DEFAULT_FLOOR : new BigDecimal(value);
    }

    /** The days after its date a claim is due by: the terms set, else 14. */
    public int terms(String sheetId) {
        String value = find(sheetId, TERMS);
        return value == null ? DEFAULT_TERMS : Integer.parseInt(value);
    }

    /** The grade's own terms (days), or null when it has none (then the sheet's terms). */
    public Integer gradeTerms(String sheetId, String grade) {
        String value = find(sheetId, TERMS_PREFIX + grade);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The days after its date a claim of a person of the grade is due by: the grade's own terms, else
     *  the sheet's. */
    public int terms(String sheetId, String grade) {
        Integer own = grade == null ? null : gradeTerms(sheetId, grade);
        return own == null ? terms(sheetId) : own;
    }

    /** The percent of an overdue claim's figure charged for every full 30 days overdue: the one set, else 0.00. */
    public BigDecimal latePct(String sheetId) {
        String value = find(sheetId, LATE_PCT);
        return value == null ? DEFAULT_LATE_PCT : new BigDecimal(value);
    }

    /** The date the sheet's rounding turns on, or null when blank (then the sheet's today). */
    public LocalDate policyDate(String sheetId) {
        String value = find(sheetId, POLICY_DATE);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The date from which claims opened are numbered per period, or null when blank (off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        String value = find(sheetId, PERIOD_NUMBERING_FROM);
        return value == null ? null : LocalDate.parse(value);
    }

    public void set(String sheetId, String key, String value) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, setting_key, setting_value) KEY (sheet_id, setting_key) "
                + "VALUES (?, ?, ?)", sheetId, key, value);
    }
}

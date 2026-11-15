package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingRepository {

    /** The clerk limit: the largest due a clerk may settle (two decimals; blank: no limit). */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The proof threshold: a claim whose due is above the one in force on its date cannot be settled
     *  or approved without a proof (two decimals; blank: none). */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** The cap: the most a person's due-total can be (two decimals; blank: no cap). */
    public static final String CAP = "cap";

    /** The floor: the least a claim's live due can be (two decimals). */
    public static final String FLOOR = "floor";

    /** The terms: the number of days after its date by which a claim is due (a whole number). */
    public static final String TERMS = "terms";

    /** A grade's own terms ({@code terms-<grade>}; blank: the sheet's terms). */
    public static final String TERMS_OF_GRADE = "terms-";

    /** The late percent: the share of an overdue claim's figure it gains for every full 30 days
     *  it is overdue (two decimals). */
    public static final String LATE_PCT = "late-pct";

    /** The policy date (yyyy-MM-dd; blank: the sheet's today): a claim dated before it rounds half
     *  up, one on or after it half to even. */
    public static final String POLICY_DATE = "policy-date";

    /** The period numbering date (yyyy-MM-dd; blank: off): once the sheet's today is on or after it,
     *  a claim opened is numbered within its period. */
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The grace days (a whole number; blank: off): when the sheet's today changes, an open period
     *  whose to-date plus them is before the new today is closed. */
    public static final String GRACE_DAYS = "grace-days";

    /** The late percent until it is first set. */
    public static final String DEFAULT_LATE_PCT = "0.00";

    /** The terms until they are first set. */
    public static final String DEFAULT_TERMS = "14";

    /** The floor until it is first set. */
    public static final String DEFAULT_FLOOR = "1.00";

    /** Whether a key is dated: every key that carries a figure (the policy date, the period
     *  numbering date, the grace days and the permission matrix are not). */
    public static boolean isDated(String key) {
        return !POLICY_DATE.equals(key) && !GRACE_DAYS.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !Permissions.isKey(key);
    }

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;

    public SettingRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** Every value recorded for the sheet's settings, by key, then by from-date (none first). */
    public List<Setting> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, setting, val, from_date FROM sheet_setting WHERE sheet_id = ? "
                + "ORDER BY setting, from_key", (rs, n) -> row(rs), sheetId);
    }

    private static Setting row(ResultSet rs) throws SQLException {
        LocalDate from = rs.getObject("from_date", LocalDate.class);
        return new Setting(rs.getString("sheet_id"), rs.getString("setting"),
                rs.getString("val") == null ? "" : rs.getString("val"), from == null ? null : from.toString());
    }

    /** The values recorded for a key, with its value before every from-date ({@code start}). */
    public DatedSetting dated(String sheetId, String key, String start) {
        return new DatedSetting(jdbc.query("SELECT sheet_id, setting, val, from_date FROM sheet_setting "
                + "WHERE sheet_id = ? AND setting = ?", (rs, n) -> row(rs), sheetId, key), start);
    }

    /** The values recorded for a key as they stood at the end of the date (each as the last value
     *  recorded while the sheet's today was on or before the date left it), with its value before
     *  every from-date ({@code start}). */
    public DatedSetting datedAsOf(String sheetId, String key, String start, LocalDate asOf) {
        return new DatedSetting(jdbc.query("SELECT sheet_id, setting, val, from_date FROM setting_history h "
                + "WHERE h.sheet_id = ? AND h.setting = ? AND h.id = (SELECT MAX(h2.id) FROM setting_history h2 "
                + "WHERE h2.sheet_id = h.sheet_id AND h2.setting = h.setting AND h2.from_key = h.from_key "
                + "AND (h2.on_date IS NULL OR h2.on_date <= ?))", (rs, n) -> row(rs), sheetId, key, asOf), start);
    }

    /** The sheet's cap in force on the date as it stood then; null when there was none. */
    public BigDecimal capAsOf(String sheetId, LocalDate asOf) {
        String value = datedAsOf(sheetId, CAP, null, asOf).on(asOf.toString());
        return value == null ? null : new BigDecimal(value);
    }

    /** What a claim's late charge and rounding went by at the end of the date, the date standing in
     *  for the sheet's today. */
    public Dues.Late lateAsOf(String sheetId, LocalDate asOf) {
        String policy = datedAsOf(sheetId, POLICY_DATE, null, asOf).on("9999-12-31");
        return new Dues.Late(datedAsOf(sheetId, LATE_PCT, DEFAULT_LATE_PCT, asOf), asOf,
                policy == null ? asOf : LocalDate.parse(policy));
    }

    /** An undated setting's value; null when blank or never set. */
    public String find(String sheetId, String key) {
        return dated(sheetId, key, null).on("9999-12-31");
    }

    /** The sheet's clerk limit in force on its today; null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = dated(sheetId, CLERK_LIMIT, null).on(sheets.today(sheetId).toString());
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's proof threshold in force on a date (yyyy-MM-dd); null when there is none. */
    public BigDecimal proofThreshold(String sheetId, String date) {
        String value = dated(sheetId, PROOF_THRESHOLD, null).on(date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's cap in force on its today; null when there is none. */
    public BigDecimal cap(String sheetId) {
        String value = dated(sheetId, CAP, null).on(sheets.today(sheetId).toString());
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's floor (two decimals) by date: a claim goes by the one in force on its date. */
    public DatedSetting floor(String sheetId) {
        return dated(sheetId, FLOOR, DEFAULT_FLOOR);
    }

    /** The terms (whole days) of a grade in force on a date: its own, else the sheet's (a null
     *  grade: the sheet's). */
    public int terms(String sheetId, String grade, String date) {
        String value = grade == null ? null : dated(sheetId, TERMS_OF_GRADE + grade, null).on(date);
        return Integer.parseInt(value != null ? value : dated(sheetId, TERMS, DEFAULT_TERMS).on(date));
    }

    /** The terms (whole days) of a claim of the person on a date: their own, else those of their
     *  grade in force on it, else the sheet's, each as in force on the date (a null person: the sheet's). */
    public int terms(String sheetId, net.officefloor.hq.app.person.Person person, String date) {
        if (person != null && person.getTerms() != null) {
            return person.getTerms();
        }
        return terms(sheetId, person == null ? null : person.gradeOn(date), date);
    }

    /** The sheet's late percent (two decimals) by date: a late charge goes by the one in force on each 30-day boundary. */
    public DatedSetting latePct(String sheetId) {
        return dated(sheetId, LATE_PCT, DEFAULT_LATE_PCT);
    }

    /** The sheet's policy date: the one set, else its {@code today}. */
    public LocalDate policyDate(String sheetId, LocalDate today) {
        String value = find(sheetId, POLICY_DATE);
        return value == null ? today : LocalDate.parse(value);
    }

    /** The sheet's period numbering date; null when off. */
    public LocalDate periodNumberingFrom(String sheetId) {
        String value = find(sheetId, PERIOD_NUMBERING_FROM);
        return value == null ? null : LocalDate.parse(value);
    }

    /** What a claim's late charge and rounding go by on the sheet now. */
    public Dues.Late late(String sheetId, LocalDate today) {
        return new Dues.Late(latePct(sheetId), today, policyDate(sheetId, today));
    }

    /** Record the setting's value (null: blank) from the from-date (null: from the beginning of time,
     *  for an undated key), replacing the one recorded from the same from-date. */
    public Setting set(String sheetId, String key, String value, LocalDate from, String user) {
        jdbc.update("DELETE FROM sheet_setting WHERE sheet_id = ? AND setting = ? AND from_key = ?",
                sheetId, key, from == null ? LocalDate.of(1, 1, 1) : from);
        jdbc.update("INSERT INTO sheet_setting (sheet_id, setting, val, from_date, set_by, set_at) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, key, value, from, user);
        jdbc.update("INSERT INTO setting_history (sheet_id, setting, val, from_date, on_date) "
                + "VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", sheetId, key, value, from, sheetId);
        return new Setting(sheetId, key, value == null ? "" : value, from == null ? null : from.toString());
    }
}

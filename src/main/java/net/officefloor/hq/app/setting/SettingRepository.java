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

    /** The sheet's late percent (two decimals) by date: a claim goes by the one in force on its date. */
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
        return new Setting(sheetId, key, value == null ? "" : value, from == null ? null : from.toString());
    }
}

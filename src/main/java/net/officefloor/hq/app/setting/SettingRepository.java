package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.Dues;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingRepository {

    /** The clerk limit: the largest due a clerk may settle (two decimals; blank: no limit). */
    public static final String CLERK_LIMIT = "clerk-limit";

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

    /** The late percent until it is first set. */
    public static final String DEFAULT_LATE_PCT = "0.00";

    /** The terms until they are first set. */
    public static final String DEFAULT_TERMS = "14";

    /** The floor until it is first set. */
    public static final String DEFAULT_FLOOR = "1.00";

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings that have been set, by key. */
    public List<Setting> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, setting, val FROM sheet_setting WHERE sheet_id = ? ORDER BY setting",
                (rs, n) -> new Setting(rs.getString("sheet_id"), rs.getString("setting"),
                        rs.getString("val") == null ? "" : rs.getString("val")), sheetId);
    }

    /** The setting's value; null when blank or never set. */
    public String find(String sheetId, String key) {
        // A blank value is a null row value, which a stream's findFirst cannot carry.
        List<String> values = jdbc.query("SELECT val FROM sheet_setting WHERE sheet_id = ? AND setting = ?",
                (rs, n) -> rs.getString("val"), sheetId, key);
        return values.isEmpty() ? null : values.get(0);
    }

    /** The sheet's clerk limit; null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = find(sheetId, CLERK_LIMIT);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's floor (two decimals). */
    public String floor(String sheetId) {
        String value = find(sheetId, FLOOR);
        return value == null ? DEFAULT_FLOOR : value;
    }

    /** The sheet's terms (whole days). */
    public int terms(String sheetId) {
        String value = find(sheetId, TERMS);
        return Integer.parseInt(value == null ? DEFAULT_TERMS : value);
    }

    /** The terms (whole days) of a grade: its own, else the sheet's (a null grade: the sheet's). */
    public int terms(String sheetId, String grade) {
        String value = grade == null ? null : find(sheetId, TERMS_OF_GRADE + grade);
        return value == null ? terms(sheetId) : Integer.parseInt(value);
    }

    /** The sheet's late percent (two decimals). */
    public String latePct(String sheetId) {
        String value = find(sheetId, LATE_PCT);
        return value == null ? DEFAULT_LATE_PCT : value;
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

    /** Set (or replace) the setting's value (null: blank). */
    public Setting set(String sheetId, String key, String value, String user) {
        jdbc.update("DELETE FROM sheet_setting WHERE sheet_id = ? AND setting = ?", sheetId, key);
        jdbc.update("INSERT INTO sheet_setting (sheet_id, setting, val, set_by, set_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, key, value, user);
        return new Setting(sheetId, key, value == null ? "" : value);
    }
}

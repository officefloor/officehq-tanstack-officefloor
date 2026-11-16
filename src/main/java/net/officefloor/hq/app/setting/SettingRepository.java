package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored settings: per sheet and key, its values each from a from-date (one undated value for an undated key). */
@Repository
public class SettingRepository {

    /** The most a clerk may settle a claim for; blank for no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The most a person's due-total may be; blank for none. */
    public static final String CAP = "cap";

    /** The due above which a claim needs a proof before it is settled or approved; blank for none. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** The least a claim's live due may be; 1.00 until set. */
    public static final String FLOOR = "floor";

    /** The floor a sheet has until one is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The days a claim has from its date until it is due by; 14 until set. */
    public static final String TERMS = "terms";

    /** The prefix of a grade's own terms: terms-A, terms-B or terms-C; a grade without its own has the sheet's terms. */
    public static final String TERMS_PREFIX = "terms-";

    /** The terms a sheet has until they are set. */
    public static final int DEFAULT_TERMS = 14;

    /** The percent of an overdue claim's figure added for every full 30 days it is overdue; 0.00 until set. */
    public static final String LATE_PCT = "late-pct";

    /** The late percent a sheet has until one is set. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The date from which a claim's live figures round half to even (before it, half up); blank: the sheet's today. */
    public static final String POLICY_DATE = "policy-date";

    /** The date from which an opened claim is numbered within its period (see ClaimNumbering); blank: off. */
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The days after a period's to-date before a change of the sheet's today closes it; blank for off (undated). */
    public static final String GRACE_DAYS = "grace-days";

    /** The prefix of an action's permission: perm-open, perm-settle, perm-void, perm-reassign, perm-close or
     *  perm-reopen, its value the roles that may take it, comma separated; blank: the standing rule (see PERMISSIONS). */
    public static final String PERM_PREFIX = "perm-";

    /** The actions with a permission, each with the roles that may take it while its permission is blank. */
    public static final Map<String, String> PERMISSIONS = Map.of(
            "open", RoleRepository.CLERK + "," + RoleRepository.STEWARD,
            "settle", RoleRepository.CLERK + "," + RoleRepository.STEWARD,
            "void", RoleRepository.CLERK + "," + RoleRepository.STEWARD,
            "close", RoleRepository.CLERK + "," + RoleRepository.STEWARD,
            "reassign", RoleRepository.STEWARD,
            "reopen", RoleRepository.STEWARD);

    /** The from-date an undated setting is stored under: before every date. */
    private static final LocalDate UNDATED = LocalDate.of(1, 1, 1);

    private final JdbcTemplate jdbc;

    private final SheetRepository sheets;

    public SettingRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's settings: every value recorded, by key then from-date. */
    public List<Setting> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value, from_date FROM setting WHERE sheet_id = ?"
                + " ORDER BY setting_key, from_date", SettingRepository::map, sheetId);
    }

    /** The setting's value in force on the date (the one with the latest from-date on or before it), or null when it
     *  has none: nothing is recorded on or before the date, or the value in force is blank. */
    public String value(String sheetId, String key, LocalDate date) {
        List<Setting> found = inForce(sheetId, key, date);
        return found.isEmpty() ? null : found.get(0).value();
    }

    /** The setting recorded in force on the date: none, or the one. */
    private List<Setting> inForce(String sheetId, String key, LocalDate date) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value, from_date FROM setting WHERE sheet_id = ?"
                + " AND setting_key = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1", SettingRepository::map,
                sheetId, key, date);
    }

    /** The undated setting's value, or null when it has none. */
    public String value(String sheetId, String key) {
        return value(sheetId, key, UNDATED);
    }

    /** The sheet's clerk limit in force on its today, or null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = value(sheetId, CLERK_LIMIT, sheets.today(sheetId));
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's cap in force on its today, or null when there is none. */
    public BigDecimal cap(String sheetId) {
        String value = value(sheetId, CAP, sheets.today(sheetId));
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's proof threshold in force on the date (a claim's date), or null when there is none. */
    public BigDecimal proofThreshold(String sheetId, LocalDate date) {
        String value = value(sheetId, PROOF_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** A due-total held to the cap: the cap when the total is above it (a null cap holds nothing). */
    public static BigDecimal capped(BigDecimal total, BigDecimal cap) {
        return cap != null && total.compareTo(cap) > 0 ? cap.setScale(2) : total;
    }

    /** The sheet's floor in force on the date (a claim's date): the least the claim's live due may be. */
    public BigDecimal floor(String sheetId, LocalDate date) {
        String value = value(sheetId, FLOOR, date);
        return value == null ? DEFAULT_FLOOR : new BigDecimal(value);
    }

    /** The sheet's terms in force on the date: the days a claim of that date has until it is due by. */
    public int terms(String sheetId, LocalDate date) {
        String value = value(sheetId, TERMS, date);
        return value == null ? DEFAULT_TERMS : Integer.parseInt(value);
    }

    /** The terms of a grade in force on the date: its own when one is recorded on or before it, else the sheet's (a
     *  null grade has the sheet's). */
    public int terms(String sheetId, String grade, LocalDate date) {
        String value = grade == null ? null : value(sheetId, TERMS_PREFIX + grade, date);
        return value == null ? terms(sheetId, date) : Integer.parseInt(value);
    }

    /** The sheet's late percent in force on the date (a 30-day boundary of an overdue claim): what the claim gains of its
     *  figure for that boundary. */
    public BigDecimal latePct(String sheetId, LocalDate date) {
        String value = value(sheetId, LATE_PCT, date);
        return value == null ? DEFAULT_LATE_PCT : new BigDecimal(value);
    }

    /** The sheet's policy date, or null when it is blank (the sheet's today applies). */
    public LocalDate policyDate(String sheetId) {
        String value = value(sheetId, POLICY_DATE);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The sheet's period numbering from-date, or null when it is blank (off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        String value = value(sheetId, PERIOD_NUMBERING_FROM);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The sheet's grace days, or null when it is blank (off). */
    public Integer graceDays(String sheetId) {
        String value = value(sheetId, GRACE_DAYS);
        return value == null ? null : Integer.valueOf(value);
    }

    /** Whether the role may take the action (open, settle, void, reassign, close or reopen) on the sheet: the roles its
     *  permission lists, or the standing rule while it is blank. */
    public boolean permits(String sheetId, String action, String role) {
        String value = value(sheetId, PERM_PREFIX + action);
        String roles = value == null ? PERMISSIONS.get(action) : value;
        return Arrays.asList(roles.split(",")).contains(role);
    }

    /** Stores the setting from its from-date (null: undated), replacing the value recorded from that date. */
    public Setting save(Setting setting) {
        LocalDate from = setting.from() == null ? UNDATED : setting.from();
        jdbc.update("DELETE FROM setting WHERE sheet_id = ? AND setting_key = ? AND from_date = ?", setting.sheetId(),
                setting.key(), from);
        jdbc.update("INSERT INTO setting (sheet_id, setting_key, setting_value, from_date) VALUES (?, ?, ?, ?)",
                setting.sheetId(), setting.key(), setting.value(), from);
        return setting;
    }

    private static Setting map(ResultSet rs, int n) throws SQLException {
        LocalDate from = rs.getObject("from_date", LocalDate.class);
        return new Setting(rs.getString("sheet_id"), rs.getString("setting_key"), rs.getString("setting_value"),
                UNDATED.equals(from) ? null : from);
    }
}

package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/** The settings on each sheet, by key; a key without a row is blank. */
@Repository
public class SettingRepository {

    private static final RowMapper<Setting> ROW = (rs, n) -> new Setting(rs.getString("sheet_id"),
            rs.getString("setting_key"), rs.getString("setting_value"));

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings, by key; the floor, the terms and the late percent show their starting values until they are set. */
    public List<Setting> list(String sheetId) {
        List<Setting> found = new java.util.ArrayList<>(jdbc.query("SELECT sheet_id, setting_key, setting_value"
                + " FROM sheet_setting WHERE sheet_id = ? ORDER BY setting_key", ROW, sheetId));
        if (found.stream().noneMatch(s -> Setting.FLOOR.equals(s.key()))) {
            found.add(new Setting(sheetId, Setting.FLOOR, Setting.FLOOR_DEFAULT));
        }
        if (found.stream().noneMatch(s -> Setting.TERMS.equals(s.key()))) {
            found.add(new Setting(sheetId, Setting.TERMS, Setting.TERMS_DEFAULT));
        }
        if (found.stream().noneMatch(s -> Setting.LATE_PCT.equals(s.key()))) {
            found.add(new Setting(sheetId, Setting.LATE_PCT, Setting.LATE_PCT_DEFAULT));
        }
        return found;
    }

    /** The setting's value; null when blank. */
    public String find(String sheetId, String key) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value FROM sheet_setting WHERE sheet_id = ?"
                + " AND setting_key = ?", ROW, sheetId, key).stream().findFirst().map(Setting::value).orElse(null);
    }

    /** The sheet's clerk limit; null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = find(sheetId, Setting.CLERK_LIMIT);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's floor: the least a claim's live due may be (1.00 until it is set). */
    public BigDecimal floor(String sheetId) {
        String value = find(sheetId, Setting.FLOOR);
        return new BigDecimal(value == null ? Setting.FLOOR_DEFAULT : value).setScale(2);
    }

    /** The sheet's terms: the days after its date a claim is due by (14 until they are set). */
    public int terms(String sheetId) {
        String value = find(sheetId, Setting.TERMS);
        return Integer.parseInt(value == null ? Setting.TERMS_DEFAULT : value);
    }

    /** The sheet's late percent (0.00 until it is set). */
    public BigDecimal latePct(String sheetId) {
        String value = find(sheetId, Setting.LATE_PCT);
        return new BigDecimal(value == null ? Setting.LATE_PCT_DEFAULT : value).setScale(2);
    }

    /** The sheet's policy date: the one set, else its today. */
    public java.time.LocalDate policyDate(String sheetId, java.time.LocalDate today) {
        String value = find(sheetId, Setting.POLICY_DATE);
        return value == null ? today : java.time.LocalDate.parse(value);
    }

    /** The sheet's period numbering from-date; null while off. */
    public java.time.LocalDate periodNumberingFrom(String sheetId) {
        String value = find(sheetId, Setting.PERIOD_NUMBERING_FROM);
        return value == null ? null : java.time.LocalDate.parse(value);
    }

    /** The sheet's late-charge terms as of its today, with its policy date for rounding. */
    public LateCharge lateCharge(String sheetId, java.time.LocalDate today) {
        return new LateCharge(today, terms(sheetId), latePct(sheetId), policyDate(sheetId, today));
    }

    /** Set a setting's value (null: blank), replacing the one there before. */
    public Setting set(String sheetId, String key, String value) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, setting_key, setting_value) KEY (sheet_id, setting_key)"
                + " VALUES (?, ?, ?)", sheetId, key, value);
        return new Setting(sheetId, key, value);
    }
}

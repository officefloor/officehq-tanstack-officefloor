package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

/**
 * The settings on each sheet, by key; a key without a row is blank. A dated setting (see {@link Setting#isDated}) keeps
 * every value saved, each from its from-date; the one in force on a date is the latest from on or before it (see
 * {@link DatedSettings}).
 */
@Repository
public class SettingRepository {

    private static final RowMapper<Setting> ROW = (rs, n) -> new Setting(rs.getString("sheet_id"),
            rs.getString("setting_key"), rs.getString("setting_value"), null);

    private static final RowMapper<DatedValue> DATED = (rs, n) -> new DatedValue(rs.getString("setting_key"),
            rs.getObject("from_date", LocalDate.class), rs.getString("setting_value"));

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's undated settings, by key (the policy date, period numbering from and the permissions); a key without a row is not listed. */
    public List<Setting> listUndated(String sheetId) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value FROM sheet_setting WHERE sheet_id = ?"
                + " ORDER BY setting_key", ROW, sheetId);
    }

    /** The sheet's dated settings: every value recorded, by key and from-date. */
    public DatedSettings dated(String sheetId) {
        return new DatedSettings(jdbc.query("SELECT setting_key, from_date, setting_value FROM sheet_setting_from"
                + " WHERE sheet_id = ? ORDER BY setting_key, from_date", DATED, sheetId));
    }

    /** The undated setting's value; null when blank. */
    public String find(String sheetId, String key) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value FROM sheet_setting WHERE sheet_id = ?"
                + " AND setting_key = ?", ROW, sheetId, key).stream().findFirst().map(Setting::value).orElse(null);
    }

    /** Whether the role may take the action (open, settle, void, reassign, close, reopen): its permission's roles, else the standing rule. */
    public boolean allows(String sheetId, String action, String role) {
        String value = find(sheetId, Setting.PERMISSION + action);
        List<String> roles = value == null ? Setting.PERMISSION_STANDING.get(action) : List.of(value.split(","));
        return roles.contains(role);
    }

    /** A person's due-total held to the cap: the cap when it is above it; n/a (null) and no cap leave it as it is. */
    public static BigDecimal capped(BigDecimal dueTotal, BigDecimal cap) {
        return dueTotal == null || cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }

    /** The sheet's policy date: the one set, else its today. */
    public LocalDate policyDate(String sheetId, LocalDate today) {
        String value = find(sheetId, Setting.POLICY_DATE);
        return value == null ? today : LocalDate.parse(value);
    }

    /** The sheet's period numbering from-date; null while off. */
    public LocalDate periodNumberingFrom(String sheetId) {
        String value = find(sheetId, Setting.PERIOD_NUMBERING_FROM);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The sheet's late charge as of its today, each claim's late percent the one in force on its date, with its policy date for rounding. */
    public LateCharge lateCharge(String sheetId, LocalDate today) {
        return new LateCharge(today, dated(sheetId), policyDate(sheetId, today));
    }

    /** Set an undated setting's value (null: blank), replacing the one there before. */
    public Setting set(String sheetId, String key, String value) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, setting_key, setting_value) KEY (sheet_id, setting_key)"
                + " VALUES (?, ?, ?)", sheetId, key, value);
        return new Setting(sheetId, key, value, null);
    }

    /** Record a dated setting's value (null: blank) from the date, keeping the others; one already from that date is replaced. */
    public void setFrom(String sheetId, String key, LocalDate from, String value) {
        jdbc.update("MERGE INTO sheet_setting_from (sheet_id, setting_key, from_date, setting_value)"
                + " KEY (sheet_id, setting_key, from_date) VALUES (?, ?, ?, ?)", sheetId, key, from, value);
    }

    /** Every setting as of the date: each dated key's value in force then (its starting value, or blank), and the undated ones set. */
    public List<SettingResponse> listOn(String sheetId, LocalDate today) {
        DatedSettings dated = dated(sheetId);
        List<SettingResponse> found = new ArrayList<>();
        java.util.Set<String> keys = new java.util.TreeSet<>(List.of(Setting.FLOOR, Setting.TERMS, Setting.LATE_PCT));
        dated.values().forEach(v -> keys.add(v.key()));
        for (String key : keys) {
            found.add(new SettingResponse(key, dated.on(key, today), dated.values().stream()
                    .filter(v -> v.key().equals(key))
                    .map(v -> new SettingResponse.From(v.from().toString(), v.value())).toList()));
        }
        listUndated(sheetId).forEach(s -> found.add(new SettingResponse(s.key(), s.value(), List.of())));
        return found;
    }
}

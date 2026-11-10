package net.officefloor.hq.app.setting;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.stereotype.Repository;

/** The settings store: the dated values in setting_value, the undated ones on the sheet's row. */
@Repository
public class SettingsRepository {

    private final JdbcTemplate jdbc;

    public SettingsRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Settings find(String sheetId) {
        Map<String, NavigableMap<LocalDate, String>> dated = new HashMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM setting_value WHERE sheet_id = ?",
                (RowCallbackHandler) (rs) -> dated.computeIfAbsent(rs.getString("setting_key"), (k) -> new TreeMap<>())
                        .put(rs.getDate("from_date").toLocalDate(), rs.getString("setting_value")), sheetId);
        Map<String, String> perms = new HashMap<>();
        jdbc.query("SELECT action, roles FROM setting_perm WHERE sheet_id = ?",
                (RowCallbackHandler) (rs) -> perms.put(rs.getString("action"), rs.getString("roles")), sheetId);
        List<Settings> found = jdbc.query("SELECT today, policy_date, period_numbering_from FROM sheet WHERE id = ?",
                (rs, n) -> new Settings(sheetId, rs.getDate("today").toLocalDate(), dated,
                        rs.getDate("policy_date") == null ? null : rs.getDate("policy_date").toLocalDate(),
                        rs.getDate("period_numbering_from") == null ? null : rs.getDate("period_numbering_from").toLocalDate(), perms),
                sheetId);
        return found.isEmpty() ? new Settings(sheetId, null, dated, null, null, perms) : found.get(0);
    }

    /** Records a dated setting's value from a date, replacing any value already recorded from it. */
    public void record(String sheetId, String key, LocalDate from, String value) {
        jdbc.update("MERGE INTO setting_value (sheet_id, setting_key, from_date, setting_value) KEY (sheet_id, setting_key, from_date) VALUES (?, ?, ?, ?)",
                sheetId, key, java.sql.Date.valueOf(from), value);
    }

    /** Records the roles ('' for the standing rule) that may take an action. */
    public void recordPerm(String sheetId, String action, String roles) {
        jdbc.update("MERGE INTO setting_perm (sheet_id, action, roles) KEY (sheet_id, action) VALUES (?, ?, ?)",
                sheetId, action, roles);
    }

    /** Saves the undated settings. */
    public void saveUndated(Settings settings) {
        jdbc.update("UPDATE sheet SET policy_date = ?, period_numbering_from = ? WHERE id = ?",
                settings.policyDate() == null ? null : java.sql.Date.valueOf(settings.policyDate()),
                settings.periodNumberingFrom() == null ? null : java.sql.Date.valueOf(settings.periodNumberingFrom()), settings.sheetId());
    }
}

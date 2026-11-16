package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored settings, one value per key per sheet. */
@Repository
public class SettingRepository {

    /** The most a clerk may settle a claim for; blank for no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings, by key. */
    public List<Setting> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value FROM setting WHERE sheet_id = ? ORDER BY setting_key",
                SettingRepository::map, sheetId);
    }

    /** The setting's value, or null when it has none. */
    public String value(String sheetId, String key) {
        List<String> found = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet_id = ? AND setting_key = ?",
                String.class, sheetId, key);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The sheet's clerk limit, or null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = value(sheetId, CLERK_LIMIT);
        return value == null ? null : new BigDecimal(value);
    }

    /** Stores the setting, replacing the value it had. */
    public Setting save(Setting setting) {
        jdbc.update("DELETE FROM setting WHERE sheet_id = ? AND setting_key = ?", setting.sheetId(), setting.key());
        jdbc.update("INSERT INTO setting (sheet_id, setting_key, setting_value) VALUES (?, ?, ?)",
                setting.sheetId(), setting.key(), setting.value());
        return setting;
    }

    private static Setting map(ResultSet rs, int n) throws SQLException {
        return new Setting(rs.getString("sheet_id"), rs.getString("setting_key"), rs.getString("setting_value"));
    }
}

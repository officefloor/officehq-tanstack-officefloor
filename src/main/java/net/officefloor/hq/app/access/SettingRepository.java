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

    /** The sheet's settings, by key. */
    public List<Setting> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value FROM sheet_setting WHERE sheet_id = ?"
                + " ORDER BY setting_key", ROW, sheetId);
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

    /** Set a setting's value (null: blank), replacing the one there before. */
    public Setting set(String sheetId, String key, String value) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, setting_key, setting_value) KEY (sheet_id, setting_key)"
                + " VALUES (?, ?, ?)", sheetId, key, value);
        return new Setting(sheetId, key, value);
    }
}

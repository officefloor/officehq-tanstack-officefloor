package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingRepository {

    /** The clerk limit: the largest due a clerk may settle (two decimals; blank: no limit). */
    public static final String CLERK_LIMIT = "clerk-limit";

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
        return jdbc.query("SELECT val FROM sheet_setting WHERE sheet_id = ? AND setting = ?",
                (rs, n) -> rs.getString("val"), sheetId, key).stream().findFirst().orElse(null);
    }

    /** The sheet's clerk limit; null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = find(sheetId, CLERK_LIMIT);
        return value == null ? null : new BigDecimal(value);
    }

    /** Set (or replace) the setting's value (null: blank). */
    public Setting set(String sheetId, String key, String value, String user) {
        jdbc.update("DELETE FROM sheet_setting WHERE sheet_id = ? AND setting = ?", sheetId, key);
        jdbc.update("INSERT INTO sheet_setting (sheet_id, setting, val, set_by, set_at) "
                + "VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, key, value, user);
        return new Setting(sheetId, key, value == null ? "" : value);
    }
}

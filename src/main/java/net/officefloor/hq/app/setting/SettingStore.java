package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings kept per sheet, by key. A setting never set, or set blank, has no value. */
@Repository
public class SettingStore {

    public static final String CLERK_LIMIT = "clerk-limit";

    private final JdbcTemplate jdbc;

    public SettingStore(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The setting's value, or null when it has none. */
    public String find(String sheetId, String key) {
        List<String> found = jdbc.queryForList(
                "SELECT setting_value FROM sheet_setting WHERE sheet_id = ? AND setting_key = ?", String.class,
                sheetId, key);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The most a clerk may settle a claim's due at, or null for no limit. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = find(sheetId, CLERK_LIMIT);
        return value == null ? null : new BigDecimal(value);
    }

    public void set(String sheetId, String key, String value) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, setting_key, setting_value) KEY (sheet_id, setting_key) "
                + "VALUES (?, ?, ?)", sheetId, key, value);
    }
}

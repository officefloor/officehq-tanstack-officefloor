package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings kept per sheet, by key; a key with no value set takes its default. */
@Repository
public class Settings {

    /** The due a clerk may settle at most (two decimals); blank: no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The least a claim's live due is (two decimals); blank: its default. */
    public static final String FLOOR = "floor";

    /** The floor a sheet starts with. */
    public static final String DEFAULT_FLOOR = "1.00";

    private final JdbcTemplate jdbc;

    public Settings(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The value set for the key, or "" when none is. */
    public String get(String sheet, String key) {
        List<String> found = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet = ? AND setting_key = ?",
                String.class, sheet, key);
        return found.isEmpty() ? "" : found.get(0);
    }

    public void set(String sheet, String key, String value) {
        jdbc.update("MERGE INTO setting (sheet, setting_key, setting_value) KEY (sheet, setting_key) VALUES (?, ?, ?)", sheet,
                key, value);
    }

    /** The sheet's clerk limit, or null when there is none. */
    public BigDecimal clerkLimit(String sheet) {
        String limit = get(sheet, CLERK_LIMIT);
        return limit.isEmpty() ? null : new BigDecimal(limit);
    }

    /** The sheet's floor: the least a claim's live due is. */
    public BigDecimal floor(String sheet) {
        String floor = get(sheet, FLOOR);
        return new BigDecimal(floor.isEmpty() ? DEFAULT_FLOOR : floor);
    }

    public SettingsResponse all(String sheet) {
        return new SettingsResponse(get(sheet, CLERK_LIMIT), floor(sheet).toPlainString());
    }
}

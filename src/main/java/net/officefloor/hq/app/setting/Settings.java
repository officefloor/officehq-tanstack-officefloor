package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of a sheet's settings and of the roles it gives user names, kept per sheet. */
@Repository
public class Settings {

    public static final String CLERK_LIMIT = "clerk-limit";
    public static final String FLOOR = "floor";
    public static final String FLOOR_DEFAULT = "1.00";
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";

    private final JdbcTemplate jdbc;

    public Settings(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The setting's value; "" when it was never set. */
    public String get(String sheetId, String key) {
        List<String> values = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet_id = ? AND setting_key = ?",
                String.class, sheetId, key);
        return values.isEmpty() ? "" : values.get(0);
    }

    public void set(String sheetId, String key, String value) {
        jdbc.update("MERGE INTO setting (sheet_id, setting_key, setting_value) KEY (sheet_id, setting_key) VALUES (?, ?, ?)",
                sheetId, key, value);
    }

    /** The clerk limit; null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = get(sheetId, CLERK_LIMIT);
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    /** The floor, with two decimals: what a claim's live due is at least; 1.00 when never set. */
    public BigDecimal floor(String sheetId) {
        String value = get(sheetId, FLOOR);
        return new BigDecimal(value.isEmpty() ? FLOOR_DEFAULT : value);
    }

    /** A live due raised to the sheet's floor; null (no due) stays null. */
    public BigDecimal floored(String sheetId, BigDecimal due) {
        return due == null ? null : due.max(floor(sheetId));
    }

    /** The sheet's roles, by user name. */
    public List<Role> roles(String sheetId) {
        return jdbc.query("SELECT * FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, n) -> new Role(rs.getString("sheet_id"), rs.getString("user_name"), rs.getString("role")), sheetId);
    }

    /** The user's role: clerk unless the sheet gave them another. */
    public String role(String sheetId, String user) {
        List<String> roles = jdbc.queryForList("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                String.class, sheetId, user);
        return roles.isEmpty() ? CLERK : roles.get(0);
    }

    public void save(Role role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                role.sheetId(), role.user(), role.role());
    }
}

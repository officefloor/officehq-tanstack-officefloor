package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingRepository {

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings: its clerk limit and its roles, by user name. */
    public Settings find(String sheetId) {
        List<BigDecimal> limit = jdbc.query("SELECT clerk_limit FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("clerk_limit"), sheetId);
        List<UserRole> roles = jdbc.query("SELECT sheet_id, user_name, role FROM user_role WHERE sheet_id = ?"
                + " ORDER BY user_name", (rs, n) -> new UserRole(rs.getString("sheet_id"), rs.getString("user_name"),
                        Role.valueOf(rs.getString("role"))), sheetId);
        BigDecimal clerkLimit = limit.isEmpty() || limit.get(0) == null ? null : limit.get(0).setScale(2);
        return new Settings(sheetId, clerkLimit, roles);
    }

    /** Set the sheet's clerk limit (null: no limit). */
    public void saveClerkLimit(String sheetId, BigDecimal clerkLimit) {
        jdbc.update("UPDATE sheet SET clerk_limit = ? WHERE id = ?", clerkLimit, sheetId);
    }

    /** Give the user name its role on the sheet, replacing any it had. */
    public void saveRole(UserRole role) {
        int updated = jdbc.update("UPDATE user_role SET role = ? WHERE sheet_id = ? AND user_name = ?",
                role.role().name(), role.sheetId(), role.user());
        if (updated == 0) {
            jdbc.update("INSERT INTO user_role (sheet_id, user_name, role) VALUES (?, ?, ?)",
                    role.sheetId(), role.user(), role.role().name());
        }
    }
}

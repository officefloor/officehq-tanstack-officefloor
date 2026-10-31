package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each sheet's settings: the roles given to user names, and its clerk limit. */
@Repository
public class Settings {

    private final JdbcTemplate jdbc;

    public Settings(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The user's role on the sheet: a clerk unless given another. */
    public Role role(String sheetId, String user) {
        return jdbc.query("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                (rs, i) -> Role.valueOf(rs.getString("role")), sheetId, user)
                .stream().findFirst().orElse(Role.CLERK);
    }

    /** Every role given on the sheet, by user name. */
    public List<RoleResponse> roles(String sheetId) {
        return jdbc.query("SELECT user_name, role FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, i) -> new RoleResponse(rs.getString("user_name"), Role.valueOf(rs.getString("role")).show()),
                sheetId);
    }

    public void setRole(String sheetId, String user, Role role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                sheetId, user, role.name());
    }

    /** The sheet's clerk limit, or null for no limit. */
    public BigDecimal clerkLimit(String sheetId) {
        return jdbc.query("SELECT clerk_limit FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getBigDecimal("clerk_limit"), sheetId)
                .stream().findFirst().orElse(null);
    }

    public void setClerkLimit(String sheetId, BigDecimal limit) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, clerk_limit) KEY (sheet_id) VALUES (?, ?)", sheetId, limit);
    }
}

package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings store: the sheet's clerk limit, its floor, its terms and the role each user name holds on it. */
@Repository
public class Settings {

    public static final String CLERK = "clerk";

    public static final String STEWARD = "steward";

    private final JdbcTemplate jdbc;

    private final Sheets sheets;

    public Settings(JdbcTemplate jdbc, Sheets sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's clerk limit; null for no limit. */
    public BigDecimal clerkLimit(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the limit
        return jdbc.queryForObject("SELECT clerk_limit FROM sheet WHERE id = ?", BigDecimal.class, sheetId);
    }

    public void setClerkLimit(String sheetId, BigDecimal limit) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET clerk_limit = ? WHERE id = ?", limit, sheetId);
    }

    /** The sheet's floor: a claim's live due is at least it. */
    public BigDecimal floor(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the floor
        return jdbc.queryForObject("SELECT due_floor FROM sheet WHERE id = ?", BigDecimal.class, sheetId);
    }

    public void setFloor(String sheetId, BigDecimal floor) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET due_floor = ? WHERE id = ?", floor, sheetId);
    }

    /** The sheet's terms in days: a claim's due-by is its date plus them. */
    public int terms(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the terms
        return jdbc.queryForObject("SELECT terms_days FROM sheet WHERE id = ?", Integer.class, sheetId);
    }

    public void setTerms(String sheetId, int days) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET terms_days = ? WHERE id = ?", days, sheetId);
    }

    /** The role the user name holds on the sheet; a clerk unless set otherwise. */
    public String role(String sheetId, String user) {
        List<String> found = jdbc.query("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                (rs, i) -> rs.getString(1), sheetId, user == null ? "" : user.trim());
        return found.isEmpty() ? CLERK : found.get(0);
    }

    public void setRole(String sheetId, String user, String role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                sheetId, user, role);
    }

    /** The sheet's settings as shown: the clerk limit ("" for none), the floor, the terms and every role set, by user name. */
    public SettingsResponse load(String sheetId) {
        BigDecimal limit = clerkLimit(sheetId);
        List<RoleResponse> roles = jdbc.query(
                "SELECT user_name, role FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, i) -> new RoleResponse(rs.getString(1), rs.getString(2)), sheetId);
        return new SettingsResponse(limit == null ? "" : limit.setScale(2).toPlainString(),
                floor(sheetId).setScale(2).toPlainString(), String.valueOf(terms(sheetId)), roles);
    }
}

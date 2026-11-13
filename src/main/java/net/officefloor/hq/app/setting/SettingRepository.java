package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.claim.LateCharge;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingRepository {

    /** The floor a sheet starts with. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The terms a sheet starts with, in days. */
    public static final int DEFAULT_TERMS = 14;

    /** The late percent a sheet starts with. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    private final JdbcTemplate jdbc;

    public SettingRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    /** The sheet's settings: its clerk limit, its floor, its terms and its roles, by user name. */
    public Settings find(String sheetId) {
        List<BigDecimal> limit = jdbc.query("SELECT clerk_limit FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("clerk_limit"), sheetId);
        List<UserRole> roles = jdbc.query("SELECT sheet_id, user_name, role FROM user_role WHERE sheet_id = ?"
                + " ORDER BY user_name", (rs, n) -> new UserRole(rs.getString("sheet_id"), rs.getString("user_name"),
                        Role.valueOf(rs.getString("role"))), sheetId);
        BigDecimal clerkLimit = limit.isEmpty() || limit.get(0) == null ? null : limit.get(0).setScale(2);
        return new Settings(sheetId, clerkLimit, floor(sheetId), terms(sheetId), latePct(sheetId), roles);
    }

    /** The sheet's floor: a claim's live due is at least it (1.00 for a sheet not yet stored). */
    public BigDecimal floor(String sheetId) {
        List<BigDecimal> floor = jdbc.query("SELECT due_floor FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("due_floor"), sheetId);
        return floor.isEmpty() || floor.get(0) == null ? DEFAULT_FLOOR : floor.get(0).setScale(2);
    }

    /** The sheet's terms in days: a claim is due by its date plus them (14 for a sheet not yet stored). */
    public int terms(String sheetId) {
        List<Integer> terms = jdbc.query("SELECT terms FROM sheet WHERE id = ?", (rs, n) -> rs.getInt("terms"), sheetId);
        return terms.isEmpty() ? DEFAULT_TERMS : terms.get(0);
    }

    /** The sheet's late percent: an overdue claim gains it of its figure every full 30 days (0.00 for a sheet not yet stored). */
    public BigDecimal latePct(String sheetId) {
        List<BigDecimal> pct = jdbc.query("SELECT late_pct FROM sheet WHERE id = ?",
                (rs, n) -> rs.getBigDecimal("late_pct"), sheetId);
        return pct.isEmpty() || pct.get(0) == null ? DEFAULT_LATE_PCT : pct.get(0).setScale(2);
    }

    /** What the sheet charges an overdue claim on the given today: its terms and its late percent. */
    public LateCharge lateCharge(String sheetId, LocalDate today) {
        return new LateCharge(terms(sheetId), latePct(sheetId), today);
    }

    /** Set the sheet's late percent. */
    public void saveLatePct(String sheetId, BigDecimal latePct) {
        jdbc.update("UPDATE sheet SET late_pct = ? WHERE id = ?", latePct, sheetId);
    }

    /** Set the sheet's terms. */
    public void saveTerms(String sheetId, int terms) {
        jdbc.update("UPDATE sheet SET terms = ? WHERE id = ?", terms, sheetId);
    }

    /** Set the sheet's floor. */
    public void saveFloor(String sheetId, BigDecimal floor) {
        jdbc.update("UPDATE sheet SET due_floor = ? WHERE id = ?", floor, sheetId);
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

package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** Each sheet's settings: the roles given to user names, its clerk limit, its floor, its terms and its late percent. */
@Repository
public class Settings {

    /** The floor of a sheet that has not set one. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The terms, in days, of a sheet that has not set them. */
    public static final int DEFAULT_TERMS = 14;

    /** The late percent of a sheet that has not set one. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

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
        return column("clerk_limit", sheetId);
    }

    public void setClerkLimit(String sheetId, BigDecimal limit) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, clerk_limit) KEY (sheet_id) VALUES (?, ?)", sheetId, limit);
    }

    /** The sheet's floor: a claim's live due is at least it. 1.00 unless set. */
    public BigDecimal floor(String sheetId) {
        BigDecimal floor = column("floor", sheetId);
        return floor != null ? floor : DEFAULT_FLOOR;
    }

    public void setFloor(String sheetId, BigDecimal floor) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, floor) KEY (sheet_id) VALUES (?, ?)", sheetId, floor);
    }

    /** The sheet's terms: a claim is due by its date plus this many days. 14 unless set. */
    public int terms(String sheetId) {
        BigDecimal terms = column("terms", sheetId);
        return terms != null ? terms.intValue() : DEFAULT_TERMS;
    }

    public void setTerms(String sheetId, int terms) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, terms) KEY (sheet_id) VALUES (?, ?)", sheetId, terms);
    }

    /** The sheet's late percent: an overdue claim's due gains it of its figure for every full 30 days
     *  overdue. 0.00 unless set. */
    public BigDecimal latePct(String sheetId) {
        BigDecimal pct = column("late_pct", sheetId);
        return pct != null ? pct : DEFAULT_LATE_PCT;
    }

    public void setLatePct(String sheetId, BigDecimal pct) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, late_pct) KEY (sheet_id) VALUES (?, ?)", sheetId, pct);
    }

    /** One setting's stored value, null when the sheet has no row or the setting is unset. */
    private BigDecimal column(String column, String sheetId) {
        List<BigDecimal> values = jdbc.query("SELECT " + column + " FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getBigDecimal(column), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }
}

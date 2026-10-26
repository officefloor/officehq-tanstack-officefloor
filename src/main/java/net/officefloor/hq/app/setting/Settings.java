package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The store of a sheet's settings and of the roles it gives user names, kept per sheet. */
@Repository
public class Settings {

    public static final String CLERK_LIMIT = "clerk-limit";
    public static final String FLOOR = "floor";
    public static final String FLOOR_DEFAULT = "1.00";
    public static final String TERMS = "terms";
    public static final String TERMS_DEFAULT = "14";
    public static final String LATE_PCT = "late-pct";
    public static final String LATE_PCT_DEFAULT = "0.00";
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

    /** The terms: the number of days after its date a claim is due by; 14 when never set. */
    public int terms(String sheetId) {
        String value = get(sheetId, TERMS);
        return Integer.parseInt(value.isEmpty() ? TERMS_DEFAULT : value);
    }

    /** The late percent, with two decimals: what an overdue claim gains per full 30 days; 0.00 when never set. */
    public BigDecimal latePct(String sheetId) {
        String value = get(sheetId, LATE_PCT);
        return new BigDecimal(value.isEmpty() ? LATE_PCT_DEFAULT : value);
    }

    /**
     * A claim's figure with its late charge added on top: an OPEN claim whose due-by (its date plus the terms) is
     * before {@code today} gains the late percent of the figure for every full 30 days it is overdue, half to even.
     * The charge is never bound by a statement, so it follows the sheet's today; null (no figure) stays null.
     */
    public BigDecimal withLate(Claim claim, BigDecimal figure, LocalDate today) {
        if (figure == null || claim.status() != ClaimStatus.OPEN) {
            return figure;
        }
        LocalDate dueBy = claim.date().plusDays(terms(claim.sheetId()));
        if (!dueBy.isBefore(today)) {
            return figure;
        }
        long periods = ChronoUnit.DAYS.between(dueBy, today) / 30;
        BigDecimal charge = figure.multiply(latePct(claim.sheetId())).multiply(BigDecimal.valueOf(periods))
                .movePointLeft(2).setScale(2, RoundingMode.HALF_EVEN);
        return figure.add(charge);
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

package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.ClaimStatus;
import net.officefloor.hq.app.claim.Claims;
import net.officefloor.hq.app.sheet.Sheets;
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
    /** The grades a sheet may give terms of their own, each kept under {@link #gradeTermsKey}. */
    public static final List<String> GRADES = List.of("A", "B", "C");
    public static final String LATE_PCT = "late-pct";
    public static final String LATE_PCT_DEFAULT = "0.00";
    public static final String POLICY_DATE = "policy-date";
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";

    private final JdbcTemplate jdbc;
    private final Sheets sheets;
    private final Claims claims;

    public Settings(JdbcTemplate jdbc, Sheets sheets, Claims claims) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.claims = claims;
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

    /** The key a grade's own terms are kept under: terms-<grade>. */
    public static String gradeTermsKey(String grade) {
        return TERMS + "-" + grade;
    }

    /** The terms of the grade: its own terms, or the sheet's terms when it has none of its own. */
    public int terms(String sheetId, String grade) {
        String value = get(sheetId, gradeTermsKey(grade));
        return value.isEmpty() ? terms(sheetId) : Integer.parseInt(value);
    }

    /** The late percent, with two decimals: what an overdue claim gains per full 30 days; 0.00 when never set. */
    public BigDecimal latePct(String sheetId) {
        String value = get(sheetId, LATE_PCT);
        return new BigDecimal(value.isEmpty() ? LATE_PCT_DEFAULT : value);
    }

    /** The policy date: the one set, or the sheet's today when it is blank. */
    public LocalDate policyDate(String sheetId) {
        String value = get(sheetId, POLICY_DATE);
        return value.isEmpty() ? sheets.today(sheetId) : LocalDate.parse(value);
    }

    /** The date from which opened claims are numbered within their period; null when it is off. */
    public LocalDate periodNumberingFrom(String sheetId) {
        String value = get(sheetId, PERIOD_NUMBERING_FROM);
        return value.isEmpty() ? null : LocalDate.parse(value);
    }

    /**
     * How the claim's live figures round: half up when it is dated before the policy date, half to even on or after it.
     * A correction rounds by the date of the claim it corrects.
     */
    public RoundingMode rounding(Claim claim) {
        LocalDate date = claim.date();
        if (claim.isCorrection() && claim.corrects() != null) {
            Claim corrected = claims.findByNumber(claim.sheetId(), claim.personId(), claim.corrects());
            if (corrected != null) {
                date = corrected.date();
            }
        }
        return date.isBefore(policyDate(claim.sheetId())) ? RoundingMode.HALF_UP : RoundingMode.HALF_EVEN;
    }

    /** The claim's live due at the rate (a percent): its amount times the rate, rounded by {@link #rounding}, raised to the floor; null (no rate) stays null. */
    public BigDecimal live(Claim claim, BigDecimal rate) {
        return rate == null ? null
                : floored(claim, claim.amount().multiply(rate).movePointLeft(2).setScale(2, rounding(claim)));
    }

    /**
     * A claim's figure with its late charge added on top: an open (OPEN or PENDING) claim whose due-by (fixed when it was made) is
     * before {@code today} gains the late percent of the figure for every full 30 days it is overdue, rounded by {@link #rounding}.
     * The charge is never bound by a statement, so it follows the sheet's today; null (no figure) stays null.
     * A correction has no late charge.
     */
    public BigDecimal withLate(Claim claim, BigDecimal figure, LocalDate today) {
        if (figure == null || !claim.status().isOpen() || claim.isCorrection()) {
            return figure;
        }
        LocalDate dueBy = claim.dueBy();
        if (!dueBy.isBefore(today)) {
            return figure;
        }
        long periods = ChronoUnit.DAYS.between(dueBy, today) / 30;
        BigDecimal charge = figure.multiply(latePct(claim.sheetId())).multiply(BigDecimal.valueOf(periods))
                .movePointLeft(2).setScale(2, rounding(claim));
        return figure.add(charge);
    }

    /** The claim's live due raised to the sheet's floor; null (no due) stays null, and a correction's due has no floor. */
    public BigDecimal floored(Claim claim, BigDecimal due) {
        return due == null || claim.isCorrection() ? due : due.max(floor(claim.sheetId()));
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

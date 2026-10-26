package net.officefloor.hq.app.setting;

import net.officefloor.hq.app.history.AsOf;
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
    public static final String CAP = "cap";
    public static final String PROOF_THRESHOLD = "proof-threshold";
    /** Days after a period's to-date it stays open once the sheet's today passes it (undated); blank is off. */
    public static final String GRACE_DAYS = "grace-days";
    /** The settings that carry a figure: each value is kept from a from-date (see {@link #on}). */
    public static final List<String> DATED = List.of(CLERK_LIMIT, FLOOR, TERMS, gradeTermsKey("A"), gradeTermsKey("B"),
            gradeTermsKey("C"), LATE_PCT, CAP, PROOF_THRESHOLD);
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    /** A role that sees everything and may change nothing; never one the permission matrix allows an action to. */
    public static final String VIEWER = "viewer";
    /** The roles a sheet gives user names. */
    public static final List<String> ROLES = List.of(CLERK, STEWARD);
    /** The actions the permission matrix decides, each kept (undated) under {@link #permKey}. */
    public static final List<String> PERM_ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    private final JdbcTemplate jdbc;
    private final Sheets sheets;
    private final Claims claims;
    private final Delegations delegations;

    public Settings(JdbcTemplate jdbc, Sheets sheets, Claims claims, Delegations delegations) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.claims = claims;
        this.delegations = delegations;
    }

    /** The setting's value; "" when it was never set. */
    public String get(String sheetId, String key) {
        List<String> values = jdbc.queryForList("SELECT setting_value FROM " + AsOf.table("setting") + " WHERE sheet_id = ? AND setting_key = ?",
                String.class, sheetId, key);
        return values.isEmpty() ? "" : values.get(0);
    }

    public void set(String sheetId, String key, String value) {
        jdbc.update("MERGE INTO setting (sheet_id, setting_key, setting_value) KEY (sheet_id, setting_key) VALUES (?, ?, ?)",
                sheetId, key, value);
    }

    /** A dated setting's value from a from-date; one already recorded from that date is replaced. */
    public void set(String sheetId, String key, LocalDate from, String value) {
        jdbc.update("MERGE INTO setting_from (sheet_id, setting_key, from_date, setting_value) KEY (sheet_id, setting_key, from_date)"
                + " VALUES (?, ?, ?, ?)", sheetId, key, java.sql.Date.valueOf(from), value);
    }

    /** A dated setting's value in force on the date: the one with the latest from-date on or before it; "" when there is none. */
    public String on(String sheetId, String key, LocalDate date) {
        List<String> values = jdbc.queryForList("SELECT setting_value FROM " + AsOf.table("setting_from") + " WHERE sheet_id = ? AND setting_key = ?"
                + " AND from_date <= ? ORDER BY from_date DESC LIMIT 1", String.class, sheetId, key, java.sql.Date.valueOf(date));
        return values.isEmpty() ? "" : values.get(0);
    }

    /** A dated setting's values, by from-date. */
    public List<DatedValue> history(String sheetId, String key) {
        return jdbc.query("SELECT from_date, setting_value FROM setting_from WHERE sheet_id = ? AND setting_key = ? ORDER BY from_date",
                (rs, n) -> new DatedValue(rs.getDate("from_date").toLocalDate().toString(), rs.getString("setting_value")),
                sheetId, key);
    }

    /** A dated setting's value from a from-date (yyyy-MM-dd). */
    public record DatedValue(String from, String value) {
    }

    /** A dated setting's value in force on the sheet's today; "" when there is none. */
    public String current(String sheetId, String key) {
        return on(sheetId, key, sheets.today(sheetId));
    }

    /** The clerk limit in force on the sheet's today; null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = current(sheetId, CLERK_LIMIT);
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    /** The cap in force on the sheet's today, with two decimals: what a person's due-total is at most; null when there is none. */
    public BigDecimal cap(String sheetId) {
        String value = current(sheetId, CAP);
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    /** The proof threshold in force on the date: a claim dated then whose due is above it is settled only with a proof; null when there is none. */
    public BigDecimal proofThreshold(String sheetId, LocalDate date) {
        String value = on(sheetId, PROOF_THRESHOLD, date);
        return value.isEmpty() ? null : new BigDecimal(value);
    }

    /** A person's due-total held to the sheet's cap; null (n/a) stays null. */
    public BigDecimal capped(String sheetId, BigDecimal dueTotal) {
        BigDecimal cap = cap(sheetId);
        return dueTotal == null || cap == null ? dueTotal : dueTotal.min(cap);
    }

    /** The floor in force on the date, with two decimals: what a claim's live due is at least; 1.00 when none is. */
    public BigDecimal floor(String sheetId, LocalDate date) {
        String value = on(sheetId, FLOOR, date);
        return new BigDecimal(value.isEmpty() ? FLOOR_DEFAULT : value);
    }

    /** The terms in force on the date: the number of days after its date a claim is due by; 14 when none are. */
    public int terms(String sheetId, LocalDate date) {
        String value = on(sheetId, TERMS, date);
        return Integer.parseInt(value.isEmpty() ? TERMS_DEFAULT : value);
    }

    /** The key a grade's own terms are kept under: terms-<grade>. */
    public static String gradeTermsKey(String grade) {
        return TERMS + "-" + grade;
    }

    /** The terms of the grade in force on the date: its own terms, or the sheet's terms when it has none of its own. */
    public int terms(String sheetId, String grade, LocalDate date) {
        String value = on(sheetId, gradeTermsKey(grade), date);
        return value.isEmpty() ? terms(sheetId, date) : Integer.parseInt(value);
    }

    /** The late percent in force on the date, with two decimals: what an overdue claim gains per full 30 days; 0.00 when none is. */
    public BigDecimal latePct(String sheetId, LocalDate date) {
        String value = on(sheetId, LATE_PCT, date);
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

    /** The grace days: how many days after its to-date an open period is closed when the sheet's today is changed; null when it is off. */
    public Integer graceDays(String sheetId) {
        String value = get(sheetId, GRACE_DAYS);
        return value.isEmpty() ? null : Integer.valueOf(value);
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

    /** The claim's rate figure at the rate (a percent): its amount times the rate, rounded by {@link #rounding}, not yet floored; null (no rate) stays null. */
    public BigDecimal live(Claim claim, BigDecimal rate) {
        return rate == null ? null : claim.amount().multiply(rate).movePointLeft(2).setScale(2, rounding(claim));
    }

    /**
     * A claim's live due from its rate figure (or the figure a statement bound): the figure with its late charge added on top, then raised
     * to the floor in force on the claim's date ({@link #floored}). An open (OPEN or PENDING) claim whose due-by (fixed when it was made) is
     * before {@code today} gains, for every full 30 days it is overdue, the late percent in force on that boundary date (the due-by plus 30, 60, 90 ... days)
     * of the figure, each charge rounded by {@link #rounding}, the charges added up; a boundary whose percent is 0.00 adds nothing.
     * The charge is never bound by a statement, so it follows the sheet's today; null (no figure) stays null.
     * A correction has no late charge and no floor, and a deferred claim no late charge (it is not overdue until the sheet's today reaches its date).
     */
    public BigDecimal withLate(Claim claim, BigDecimal figure, LocalDate today) {
        return floored(claim, lateAdded(claim, figure, today));
    }

    /** The figure with the claim's late charge on {@code today} added on top (see {@link #withLate}), not floored. */
    private BigDecimal lateAdded(Claim claim, BigDecimal figure, LocalDate today) {
        if (figure == null || !claim.status().isOpen() || claim.isCorrection() || claim.deferredOn(today)) {
            return figure;
        }
        LocalDate dueBy = claim.dueBy();
        if (!dueBy.isBefore(today)) {
            return figure;
        }
        long periods = ChronoUnit.DAYS.between(dueBy, today) / 30;
        RoundingMode rounding = rounding(claim);
        BigDecimal total = figure;
        for (long n = 1; n <= periods; n++) {
            BigDecimal pct = latePct(claim.sheetId(), dueBy.plusDays(30 * n));
            if (pct.signum() != 0) {
                total = total.add(figure.multiply(pct).movePointLeft(2).setScale(2, rounding));
            }
        }
        return total;
    }

    /** The claim's due (its figure plus its late charge) raised to the floor in force on the claim's date; null (no due) stays null, and a correction's due has no floor. */
    public BigDecimal floored(Claim claim, BigDecimal due) {
        return due == null || claim.isCorrection() ? due : due.max(floor(claim.sheetId(), claim.date()));
    }

    /** The key an action's permitted roles are kept under: perm-<action>. */
    public static String permKey(String action) {
        return "perm-" + action;
    }

    /** The roles an action is permitted to, as saved: a comma separated list, or "" for the standing rule. */
    public String perm(String sheetId, String action) {
        return get(sheetId, permKey(action));
    }

    /** The standing rule: reassign and reopen by steward; open, settle, void and close by clerk and steward. */
    private static List<String> standing(String action) {
        return action.equals("reassign") || action.equals("reopen") ? List.of(STEWARD) : ROLES;
    }

    /** Whether the user's role may take the action on the sheet: as the permission matrix has it, else by the standing rule. */
    public boolean permits(String sheetId, String action, String user) {
        String saved = perm(sheetId, action);
        List<String> roles = saved.isEmpty() ? standing(action) : List.of(saved.split(","));
        return roles.contains(role(sheetId, user));
    }

    /** The sheet's roles, by user name. */
    public List<Role> roles(String sheetId) {
        return jdbc.query("SELECT * FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, n) -> new Role(rs.getString("sheet_id"), rs.getString("user_name"), rs.getString("role")), sheetId);
    }

    /** The role the user acts in: steward while the sheet's today is inside a delegation to them, else the role the sheet gave them. */
    public String role(String sheetId, String user) {
        return delegator(sheetId, user) != null ? STEWARD : givenRole(sheetId, user);
    }

    /** The steward who delegated the steward role to the user for the sheet's today; null when none did. */
    public String delegator(String sheetId, String user) {
        String name = user == null ? "" : user.trim();
        return name.isEmpty() ? null : delegations.delegator(sheetId, name, sheets.today(sheetId));
    }

    /** The role the sheet gave the user: clerk unless it gave them another (a delegation aside). */
    public String givenRole(String sheetId, String user) {
        List<String> roles = jdbc.queryForList("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                String.class, sheetId, user);
        return roles.isEmpty() ? CLERK : roles.get(0);
    }

    /** Whether the user is a viewer on the sheet, and so may change nothing. */
    public boolean isViewer(String sheetId, String user) {
        return VIEWER.equals(role(sheetId, user == null ? "" : user.trim()));
    }

    public void save(Role role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                role.sheetId(), role.user(), role.role());
    }
}

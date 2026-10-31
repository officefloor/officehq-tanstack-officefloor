package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.jdbc.core.RowCallbackHandler;
import org.springframework.jdbc.core.JdbcTemplate;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.stereotype.Repository;

/** Each sheet's settings: the roles given to user names, its policy date, its period numbering from,
 *  and its dated settings — its clerk limit, its floor, its terms (and any per grade), its late percent
 *  its cap and its proof threshold — each recorded from from-dates. */
@Repository
public class Settings {

    /** The floor of a sheet that has not set one. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The terms, in days, of a sheet that has not set them. */
    public static final int DEFAULT_TERMS = 14;

    /** The late percent of a sheet that has not set one. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The keys of the dated settings. */
    public static final String CLERK_LIMIT = "clerk-limit", FLOOR = "floor", TERMS = "terms",
            LATE_PCT = "late-pct", CAP = "cap",
            PROOF_THRESHOLD = "proof-threshold";

    /** The actions of the permission matrix, in order. */
    public static final List<String> PERM_ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    /** The key a permission is set under: perm-<action>. */
    public static String permKey(String action) {
        return "perm-" + action;
    }

    private final JdbcTemplate jdbc;
    private final Sheets sheets;

    public Settings(JdbcTemplate jdbc, Sheets sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
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

    /** The sheet's clerk limit in force on its today, or null for no limit. */
    public BigDecimal clerkLimit(String sheetId) {
        return valueOn(sheetId, CLERK_LIMIT, sheets.today(sheetId), null);
    }

    /** The sheet's floor in force on `date`: a claim's live due is at least the one on its date.
     *  1.00 before any is recorded. */
    public BigDecimal floor(String sheetId, LocalDate date) {
        return valueOn(sheetId, FLOOR, date, DEFAULT_FLOOR);
    }

    /** The sheet's terms in force on `date`: a claim is due by its date plus those on its date. 14
     *  before any are recorded. */
    public int terms(String sheetId, LocalDate date) {
        return valueOn(sheetId, TERMS, date, BigDecimal.valueOf(DEFAULT_TERMS)).intValue();
    }

    /** The terms of `grade` in force on `date`: its own when set then, else the sheet's terms then. */
    public int terms(String sheetId, String grade, LocalDate date) {
        BigDecimal own = valueOn(sheetId, gradeKey(grade), date, null);
        return own != null ? own.intValue() : terms(sheetId, date);
    }

    /** Every grade's own terms in force on the sheet's today, by grade (a grade not in it uses the sheet's). */
    public Map<String, Integer> gradeTerms(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        Map<String, Integer> terms = new TreeMap<>();
        for (String grade : List.of("A", "B", "C")) {
            BigDecimal own = valueOn(sheetId, gradeKey(grade), today, null);
            if (own != null) terms.put(grade, own.intValue());
        }
        return terms;
    }

    /** The key a grade's own terms are recorded under. */
    public static String gradeKey(String grade) {
        return TERMS + "-" + grade;
    }

    /** The sheet's late percent in force on `date`: an overdue claim's due gains the one in force on
     *  each 30-day boundary after its due-by of its figure. 0.00 before any is recorded. */
    public BigDecimal latePct(String sheetId, LocalDate date) {
        return valueOn(sheetId, LATE_PCT, date, DEFAULT_LATE_PCT);
    }

    /** Record a dated setting's value (null for blank) from `from`, replacing one recorded from that date. */
    public void setDated(String sheetId, String key, LocalDate from, BigDecimal value) {
        jdbc.update("MERGE INTO sheet_setting_dated (sheet_id, setting_key, from_date, setting_value) "
                + "KEY (sheet_id, setting_key, from_date) VALUES (?, ?, ?, ?)",
                sheetId, key, Date.valueOf(from), value);
    }

    /** Every value recorded for the sheet's dated settings, by key, oldest from-date first. */
    public Map<String, List<DatedValue>> dated(String sheetId) {
        Map<String, List<DatedValue>> dated = new TreeMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM sheet_setting_dated WHERE sheet_id = ? "
                + "ORDER BY setting_key, from_date",
                (RowCallbackHandler) rs -> {
                    BigDecimal value = rs.getBigDecimal("setting_value");
                    dated.computeIfAbsent(rs.getString("setting_key"), k -> new ArrayList<>())
                            .add(new DatedValue(rs.getDate("from_date").toLocalDate().toString(),
                                    value != null ? value.toPlainString() : null));
                }, sheetId);
        return dated;
    }

    /** A dated setting's value from a from-date (yyyy-mm-dd); null for blank. */
    public record DatedValue(String from, String value) {
    }

    /** The value of `key` in force on `date`: the one with the latest from-date on or before it
     *  (null when that is blank), else `start`. */
    private BigDecimal valueOn(String sheetId, String key, LocalDate date, BigDecimal start) {
        List<BigDecimal> values = jdbc.query("SELECT setting_value FROM sheet_setting_dated "
                + "WHERE sheet_id = ? AND setting_key = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1",
                (rs, i) -> rs.getBigDecimal("setting_value"), sheetId, key, Date.valueOf(date));
        if (values.isEmpty()) return start;
        BigDecimal value = values.get(0);
        return value != null || start == null ? value : start;
    }

    /** The sheet's policy date as set: a claim dated before it rounds half up, one on or after it
     *  half to even. Null when unset: then the policy date is the sheet's today. */
    public LocalDate policyDate(String sheetId) {
        List<Date> values = jdbc.query("SELECT policy_date FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getDate("policy_date"), sheetId);
        return values.isEmpty() || values.get(0) == null ? null : values.get(0).toLocalDate();
    }

    public void setPolicyDate(String sheetId, LocalDate date) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, policy_date) KEY (sheet_id) VALUES (?, ?)",
                sheetId, date != null ? Date.valueOf(date) : null);
    }

    /** The sheet's period numbering from: while set and the sheet's today is on or after it, a
     *  claim opened is numbered within its period. Null when unset (off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        List<Date> values = jdbc.query("SELECT period_numbering_from FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getDate("period_numbering_from"), sheetId);
        return values.isEmpty() || values.get(0) == null ? null : values.get(0).toLocalDate();
    }

    public void setPeriodNumberingFrom(String sheetId, LocalDate date) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, period_numbering_from) KEY (sheet_id) VALUES (?, ?)",
                sheetId, date != null ? Date.valueOf(date) : null);
    }

    /** The sheet's grace days: when its today is changed, every open period whose to-date plus them
     *  is before the new today is closed. Null when unset (off). Not dated. */
    public Integer graceDays(String sheetId) {
        List<Integer> values = jdbc.query("SELECT grace_days FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> (Integer) rs.getObject("grace_days", Integer.class), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }

    public void setGraceDays(String sheetId, Integer days) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, grace_days) KEY (sheet_id) VALUES (?, ?)", sheetId, days);
    }

    /** The sheet's cap in force on its today: a person's due-total is at most it. Null for no cap. */
    public BigDecimal cap(String sheetId) {
        return valueOn(sheetId, CAP, sheets.today(sheetId), null);
    }

    /** The sheet's proof threshold in force on `date`: a claim dated then whose due is above it
     *  cannot be settled or approved while it has no proofs. Null for none. */
    public BigDecimal proofThreshold(String sheetId, LocalDate date) {
        return valueOn(sheetId, PROOF_THRESHOLD, date, null);
    }

    /** A person's due-total at most the sheet's cap; n/a (null) is not capped. */
    public BigDecimal capped(String sheetId, BigDecimal dueTotal) {
        BigDecimal cap = cap(sheetId);
        return dueTotal == null || cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }

    /** The roles set for `action` on the sheet, comma separated; null when blank (the standing rule). */
    public String permission(String sheetId, String action) {
        List<String> values = jdbc.query("SELECT roles FROM sheet_permission WHERE sheet_id = ? AND action_key = ?",
                (rs, i) -> rs.getString("roles"), sheetId, action);
        return values.isEmpty() ? null : values.get(0);
    }

    /** Every permission set on the sheet, by action (one not in it keeps the standing rule). */
    public Map<String, String> permissions(String sheetId) {
        Map<String, String> perms = new TreeMap<>();
        jdbc.query("SELECT action_key, roles FROM sheet_permission WHERE sheet_id = ? AND roles IS NOT NULL",
                (RowCallbackHandler) rs -> perms.put(rs.getString("action_key"), rs.getString("roles")), sheetId);
        return perms;
    }

    /** Set the roles that may take `action` (null for blank: the standing rule). */
    public void setPermission(String sheetId, String action, String roles) {
        jdbc.update("MERGE INTO sheet_permission (sheet_id, action_key, roles) KEY (sheet_id, action_key) VALUES (?, ?, ?)",
                sheetId, action, roles);
    }

    /** Whether the user's role may take `action` on the sheet: the roles set for it, else the standing
     *  rule — open, settle, void and close by clerk and steward; reassign and reopen by steward. A
     *  viewer may take none. */
    public boolean allows(String sheetId, String action, String user) {
        Role role = role(sheetId, user);
        if (role == Role.VIEWER) return false;
        String roles = permission(sheetId, action);
        if (roles == null) {
            return role == Role.STEWARD || !(action.equals("reassign") || action.equals("reopen"));
        }
        for (String r : roles.split(",")) {
            if (r.trim().equalsIgnoreCase(role.name())) return true;
        }
        return false;
    }
}

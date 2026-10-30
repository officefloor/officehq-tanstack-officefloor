package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.sheet.Sheets;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings store: the sheet's dated settings (clerk limit, floor, terms, each grade's terms, late percent, cap), its policy date, its period numbering from and the role each user name holds on it. */
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

    /** The sheet's starting floor: in force before its first from-date. */
    public static final BigDecimal START_FLOOR = new BigDecimal("1.00");

    /** The sheet's starting late percent. */
    public static final BigDecimal START_LATE_PCT = new BigDecimal("0.00");

    /** The sheet's starting terms in days. */
    public static final int START_TERMS = 14;

    /** A dated setting's value in force on a date: the one with the latest from-date on or before it. */
    private record InForce(BigDecimal value) {
    }

    /** The value of a dated setting in force on the date; null when nothing is recorded on or before it. */
    private InForce on(String sheetId, String key, LocalDate on) {
        List<InForce> found = jdbc.query(
                "SELECT setting_value FROM sheet_setting WHERE sheet_id = ? AND setting_key = ? AND from_date <= ?"
                        + " ORDER BY from_date DESC LIMIT 1",
                (rs, i) -> new InForce(rs.getBigDecimal(1)), sheetId, key, Date.valueOf(on));
        return found.isEmpty() ? null : found.get(0);
    }

    /** Records a dated setting's value from a date, replacing a value already recorded from that date. */
    public void setFrom(String sheetId, String key, LocalDate from, BigDecimal value) {
        sheets.today(sheetId);
        jdbc.update("MERGE INTO sheet_setting (sheet_id, setting_key, from_date, setting_value)"
                + " KEY (sheet_id, setting_key, from_date) VALUES (?, ?, ?, ?)", sheetId, key, Date.valueOf(from), value);
    }

    /** Every value recorded for the sheet's dated settings, by key, oldest from-date first. */
    public java.util.Map<String, List<DatedValue>> dated(String sheetId) {
        java.util.Map<String, List<DatedValue>> all = new java.util.TreeMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM sheet_setting WHERE sheet_id = ? ORDER BY setting_key, from_date",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    BigDecimal value = rs.getBigDecimal(3);
                    String shown = value == null ? "" : rs.getString(1).startsWith("terms")
                            ? String.valueOf(value.intValue()) : value.setScale(2).toPlainString();
                    all.computeIfAbsent(rs.getString(1), k -> new java.util.ArrayList<>())
                            .add(new DatedValue(rs.getDate(2).toLocalDate().toString(), shown));
                }, sheetId);
        return all;
    }

    /** The sheet's clerk limit in force on its today; null for no limit. */
    public BigDecimal clerkLimit(String sheetId) {
        InForce found = on(sheetId, ValidateSetting.CLERK_LIMIT, sheets.today(sheetId));
        return found == null ? null : found.value();
    }

    /** The sheet's cap in force on its today: a person's due-total is at most it; null for no cap. */
    public BigDecimal cap(String sheetId) {
        InForce found = on(sheetId, ValidateSetting.CAP, sheets.today(sheetId));
        return found == null ? null : found.value();
    }

    /** The sheet's proof threshold in force on a date (a claim's: its date); null for none. */
    public BigDecimal proofThreshold(String sheetId, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.PROOF_THRESHOLD, on);
        return found == null ? null : found.value();
    }

    /** A due-total held to the sheet's cap: the cap when above it; null (n/a) stays null. */
    public static BigDecimal capped(BigDecimal dueTotal, BigDecimal cap) {
        return dueTotal == null || cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }

    /** The sheet's floor in force on a date (a claim's: its date): a claim's live due is at least it. */
    public BigDecimal floor(String sheetId, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.FLOOR, on);
        return found == null || found.value() == null ? START_FLOOR : found.value();
    }

    /** The sheet's terms in days in force on a date: a claim's due-by is its date plus them. */
    public int terms(String sheetId, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.TERMS, on);
        return found == null || found.value() == null ? START_TERMS : found.value().intValue();
    }

    /** A grade's own terms in days in force on a date; null when it has none then (it uses the sheet's terms). */
    public Integer gradeTerms(String sheetId, String grade, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.GRADE_TERMS + grade, on);
        return found == null || found.value() == null ? null : found.value().intValue();
    }

    /** The terms a claim of a person of this grade dated on the date takes: the grade's own then, else the sheet's then. */
    public int termsFor(String sheetId, String grade, LocalDate on) {
        Integer own = grade == null ? null : gradeTerms(sheetId, grade, on);
        return own != null ? own : terms(sheetId, on);
    }

    /** The sheet's late percent in force on a date (a claim's: its date): an overdue claim gains it of its figure
     *  for every full 30 days overdue. */
    public BigDecimal latePct(String sheetId, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.LATE_PCT, on);
        return found == null || found.value() == null ? START_LATE_PCT : found.value();
    }

    /** The sheet's policy date as set; null when blank. */
    public LocalDate policyDateSet(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the policy date
        Date date = jdbc.queryForObject("SELECT policy_date FROM sheet WHERE id = ?", Date.class, sheetId);
        return date == null ? null : date.toLocalDate();
    }

    /** The sheet's policy date: a claim dated before it rounds half up, on or after it half to even.
     *  When blank it is the sheet's today. */
    public LocalDate policyDate(String sheetId) {
        LocalDate set = policyDateSet(sheetId);
        return set != null ? set : sheets.today(sheetId);
    }

    public void setPolicyDate(String sheetId, LocalDate date) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET policy_date = ? WHERE id = ?", date == null ? null : Date.valueOf(date), sheetId);
    }

    /** The date from which claims opened are numbered within their period; null when off. */
    public LocalDate periodNumberingFrom(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the date
        Date date = jdbc.queryForObject("SELECT period_numbering_from FROM sheet WHERE id = ?", Date.class, sheetId);
        return date == null ? null : date.toLocalDate();
    }

    public void setPeriodNumberingFrom(String sheetId, LocalDate date) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET period_numbering_from = ? WHERE id = ?", date == null ? null : Date.valueOf(date), sheetId);
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

    /** The roles set for an action ("" for the standing rule). */
    public String permission(String sheetId, String action) {
        List<String> found = jdbc.query("SELECT roles FROM sheet_permission WHERE sheet_id = ? AND action = ?",
                (rs, i) -> rs.getString(1), sheetId, action);
        return found.isEmpty() ? "" : found.get(0);
    }

    public void setPermission(String sheetId, String action, String roles) {
        sheets.today(sheetId);
        if (roles.isEmpty()) {
            jdbc.update("DELETE FROM sheet_permission WHERE sheet_id = ? AND action = ?", sheetId, action);
        } else {
            jdbc.update("MERGE INTO sheet_permission (sheet_id, action, roles) KEY (sheet_id, action) VALUES (?, ?, ?)",
                    sheetId, action, roles);
        }
    }

    /** Whether the user's role may take the action on the sheet: by its permission when set, else by the standing
     *  rule (open, settle, void and close by clerk and steward; reassign and reopen by steward). */
    public boolean allows(String sheetId, String user, String action) {
        String role = role(sheetId, user);
        String set = permission(sheetId, action);
        if (set.isEmpty()) {
            return STEWARD.equals(role) || !("reassign".equals(action) || "reopen".equals(action));
        }
        return List.of(set.split(",")).contains(role);
    }

    /** The sheet's settings as shown: each dated one as in force on the sheet's today — the clerk limit ("" for none),
     *  the floor, the terms, the late percent, each grade's own terms (a grade with none then is left out) and the cap
     *  ("" for none) — the proof threshold ("" for none), the policy date ("" for blank), the period numbering from ("" for off), every role set, by user
     *  name, every value recorded for the dated settings and each action's permission ("" for the standing rule). */
    public SettingsResponse load(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        BigDecimal limit = clerkLimit(sheetId);
        BigDecimal cap = cap(sheetId);
        BigDecimal threshold = proofThreshold(sheetId, today);
        LocalDate policy = policyDateSet(sheetId);
        LocalDate numbering = periodNumberingFrom(sheetId);
        List<RoleResponse> roles = jdbc.query(
                "SELECT user_name, role FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, i) -> new RoleResponse(rs.getString(1), rs.getString(2)), sheetId);
        java.util.Map<String, String> gradeTerms = new java.util.TreeMap<>();
        for (String grade : ValidateSetting.GRADES) {
            Integer own = gradeTerms(sheetId, grade, today);
            if (own != null) {
                gradeTerms.put(grade, String.valueOf(own));
            }
        }
        java.util.Map<String, String> perms = new java.util.LinkedHashMap<>();
        for (String action : ValidateSetting.ACTIONS) {
            perms.put(action, permission(sheetId, action));
        }
        return new SettingsResponse(limit == null ? "" : limit.setScale(2).toPlainString(),
                floor(sheetId, today).setScale(2).toPlainString(), String.valueOf(terms(sheetId, today)),
                latePct(sheetId, today).setScale(2).toPlainString(), policy == null ? "" : policy.toString(),
                numbering == null ? "" : numbering.toString(), roles, gradeTerms,
                cap == null ? "" : cap.setScale(2).toPlainString(), dated(sheetId),
                threshold == null ? "" : threshold.setScale(2).toPlainString(), perms);
    }
}

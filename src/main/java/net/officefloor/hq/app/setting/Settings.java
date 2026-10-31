package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.util.Objects;
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

    /** Sees every screen and figure and changes nothing. */
    public static final String VIEWER = "viewer";

    /** Acts as a clerk, but sees on the people list only the people of the crews they lead or belong to and themselves. */
    public static final String RESTRICTED = "restricted";

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
        jdbc.update("INSERT INTO setting_history (sheet_id, set_on, setting_key, from_date, setting_value) VALUES (?, (SELECT today FROM sheet WHERE id = ?), ?, ?, ?)",
                sheetId, sheetId, key, Date.valueOf(from), value);
    }

    /** Every value recorded for the sheet's dated settings, by key, oldest from-date first. */
    public java.util.Map<String, List<DatedValue>> dated(String sheetId) {
        java.util.Map<String, List<DatedValue>> all = new java.util.TreeMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM sheet_setting WHERE sheet_id = ? ORDER BY setting_key, from_date",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> {
                    BigDecimal value = rs.getBigDecimal(3);
                    String shown = value == null ? "" : ValidateSetting.ROUNDING.equals(rs.getString(1)) ? net.officefloor.hq.app.claim.Rounding.name(value)
                            : rs.getString(1).startsWith("terms") || rs.getString(1).startsWith(ValidateSetting.GRADE_GRACE)
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

    /** The clerk limit a claim of a person of this grade takes on the sheet's today: the grade's own in force then
     *  when one is set, else the sheet's; null for no limit. */
    public BigDecimal clerkLimitFor(String sheetId, String grade) {
        InForce own = grade == null ? null : on(sheetId, ValidateSetting.GRADE_CLERK_LIMIT + grade, sheets.today(sheetId));
        return own != null && own.value() != null ? own.value() : clerkLimit(sheetId);
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

    /** The proof threshold a claim of a person of this grade dated on the date takes: the grade's own in force then
     *  when one is set, else the sheet's then; null for none. */
    public BigDecimal proofThresholdFor(String sheetId, String grade, LocalDate on) {
        InForce own = grade == null ? null : on(sheetId, ValidateSetting.GRADE_PROOF_THRESHOLD + grade, on);
        return own != null && own.value() != null ? own.value() : proofThreshold(sheetId, on);
    }

    /** The sheet's note threshold in force on a date (a claim's: its date); null for none. */
    public BigDecimal noteThreshold(String sheetId, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.NOTE_THRESHOLD, on);
        return found == null ? null : found.value();
    }

    /** The sheet's review threshold in force on a date (a claim's: its date); null for none. */
    public BigDecimal reviewThreshold(String sheetId, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.REVIEW_THRESHOLD, on);
        return found == null ? null : found.value();
    }

    /** A due-total held to the sheet's cap: the cap when above it; null (n/a) stays null. */
    public static BigDecimal capped(BigDecimal dueTotal, BigDecimal cap) {
        return dueTotal == null || cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }

    /** The sheet's rounding mode in force on a date (a claim's: its rounding date); null for none (it rounds by the policy date). */
    public java.math.RoundingMode roundingMode(String sheetId, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.ROUNDING, on);
        return found == null ? null : net.officefloor.hq.app.claim.Rounding.mode(found.value());
    }

    /** The sheet's floor in force on a date (a claim's: its date): a claim's live due is at least it. */
    public BigDecimal floor(String sheetId, LocalDate on) {
        InForce found = on(sheetId, ValidateSetting.FLOOR, on);
        return found == null || found.value() == null ? START_FLOOR : found.value();
    }

    /** The sheet's terms in business days in force on a date: a claim's due-by is that many business days after its date. */
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

    /** The terms a claim of a person dated on the date takes: the person's own terms, else as {@link #termsFor(String, String, LocalDate)}. */
    public int termsFor(String sheetId, Integer personTerms, String grade, LocalDate on) {
        return personTerms != null ? personTerms : termsFor(sheetId, grade, on);
    }

    /** The terms a claim of the person created now takes: the terms in force on the day it is created (the sheet's
     *  today), not on its date — the person's own terms then, else the terms of the grade they had then, else the sheet's then. */
    public int termsOnCreation(String sheetId, net.officefloor.hq.app.person.Person person) {
        LocalDate today = sheets.today(sheetId);
        return termsFor(sheetId, person.termsOn(today), person.gradeOn(today), today);
    }

    /** A grade's grace days in force on a date (a claim's: its date); 0 when it has none then. */
    public int grace(String sheetId, String grade, LocalDate on) {
        if (grade == null) {
            return 0;
        }
        InForce found = on(sheetId, ValidateSetting.GRADE_GRACE + grade, on);
        return found == null || found.value() == null ? 0 : found.value().intValue();
    }

    /** The sheet's late percent in force on a date (a late charge's: each 30-day boundary of an overdue
     *  claim, its due-by plus 30, 60, 90 ... days, adds it of the claim's figure). */
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
        jdbc.update("INSERT INTO policy_history (sheet_id, set_on, policy_date) VALUES (?, (SELECT today FROM sheet WHERE id = ?), ?)",
                sheetId, sheetId, date == null ? null : Date.valueOf(date));
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
        plainHistory(sheetId, NUMBERING, date == null ? "" : date.toString());
    }

    /** The sheet's entry date; null when off. */
    public LocalDate entryDate(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the entry date
        Date date = jdbc.queryForObject("SELECT entry_date FROM sheet WHERE id = ?", Date.class, sheetId);
        return date == null ? null : date.toLocalDate();
    }

    public void setEntryDate(String sheetId, LocalDate date) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET entry_date = ? WHERE id = ?", date == null ? null : Date.valueOf(date), sheetId);
        plainHistory(sheetId, ENTRY, date == null ? "" : date.toString());
    }

    /** The default date of a new claim: the entry date while set, else the sheet's today. */
    public LocalDate claimDate(String sheetId) {
        LocalDate entry = entryDate(sheetId);
        return entry != null ? entry : sheets.today(sheetId);
    }

    /** The sheet's grace days; null when off. */
    public Integer graceDays(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the grace days
        return jdbc.queryForObject("SELECT grace_days FROM sheet WHERE id = ?", Integer.class, sheetId);
    }

    public void setGraceDays(String sheetId, Integer days) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET grace_days = ? WHERE id = ?", days, sheetId);
        plainHistory(sheetId, GRACE, days == null ? "" : String.valueOf(days));
    }

    /** The sheet's escalation gap in days; null when off. */
    public Integer escalateAfter(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the escalation gap
        return jdbc.queryForObject("SELECT escalate_after FROM sheet WHERE id = ?", Integer.class, sheetId);
    }

    public void setEscalateAfter(String sheetId, Integer days) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET escalate_after = ? WHERE id = ?", days, sheetId);
        plainHistory(sheetId, ESCALATE, days == null ? "" : String.valueOf(days));
    }

    /** The sheet's review SLA in days; null when none. */
    public Integer reviewSla(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the review SLA
        return jdbc.queryForObject("SELECT review_sla FROM sheet WHERE id = ?", Integer.class, sheetId);
    }

    public void setReviewSla(String sheetId, Integer days) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET review_sla = ? WHERE id = ?", days, sheetId);
        plainHistory(sheetId, REVIEW_SLA, days == null ? "" : String.valueOf(days));
    }

    /** The sheet's proof kinds, in the order set; empty when none. */
    public List<String> proofKinds(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the proof kinds
        String kinds = jdbc.queryForObject("SELECT proof_kinds FROM sheet WHERE id = ?", String.class, sheetId);
        return kinds == null || kinds.isEmpty() ? List.of() : List.of(kinds.split(","));
    }

    public void setProofKinds(String sheetId, String kinds) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET proof_kinds = ? WHERE id = ?", kinds.isEmpty() ? null : kinds, sheetId);
        plainHistory(sheetId, PROOF_KINDS, kinds);
    }

    /** The sheet's default grade as set; null when blank (A). */
    public String defaultGradeSet(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the default grade
        return jdbc.queryForObject("SELECT default_grade FROM sheet WHERE id = ?", String.class, sheetId);
    }

    /** The grade a new person takes when none is given: the sheet's default grade, A when blank. */
    public String defaultGrade(String sheetId) {
        String grade = defaultGradeSet(sheetId);
        return grade == null ? START_DEFAULT_GRADE : grade;
    }

    public void setDefaultGrade(String sheetId, String grade) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET default_grade = ? WHERE id = ?", grade.isEmpty() ? null : grade, sheetId);
        plainHistory(sheetId, DEFAULT_GRADE, grade);
    }

    public static final String START_DEFAULT_GRADE = "A";

    /** The sheet's pot at two decimals; null when none. */
    public BigDecimal pot(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the pot
        BigDecimal pot = jdbc.queryForObject("SELECT pot FROM sheet WHERE id = ?", BigDecimal.class, sheetId);
        return pot == null ? null : pot.setScale(2);
    }

    public void setPot(String sheetId, BigDecimal pot) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET pot = ? WHERE id = ?", pot, sheetId);
        plainHistory(sheetId, POT, pot == null ? "" : pot.setScale(2).toPlainString());
    }

    /** The sheet's alert threshold at two decimals; null when off. */
    public BigDecimal alertThreshold(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the alert threshold
        BigDecimal threshold = jdbc.queryForObject("SELECT alert_threshold FROM sheet WHERE id = ?", BigDecimal.class, sheetId);
        return threshold == null ? null : threshold.setScale(2);
    }

    public void setAlertThreshold(String sheetId, BigDecimal threshold) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET alert_threshold = ? WHERE id = ?", threshold, sheetId);
        plainHistory(sheetId, ALERT, threshold == null ? "" : threshold.setScale(2).toPlainString());
    }

    /** The month (1 to 12) the sheet's fiscal year starts in; 1 unless set. */
    public int fiscalStartMonth(String sheetId) {
        sheets.today(sheetId); // the sheet's row holds the fiscal start month
        Integer month = jdbc.queryForObject("SELECT fiscal_start_month FROM sheet WHERE id = ?", Integer.class, sheetId);
        return month == null ? 1 : month;
    }

    public void setFiscalStartMonth(String sheetId, int month) {
        sheets.today(sheetId);
        jdbc.update("UPDATE sheet SET fiscal_start_month = ? WHERE id = ?", month, sheetId);
        plainHistory(sheetId, FISCAL, String.valueOf(month));
    }

    /** The sheet's parent sheet id; null when none. */
    public String parent(String sheetId) {
        return sheets.parent(sheetId);
    }

    public void setParent(String sheetId, String parent) {
        sheets.setParent(sheetId, parent);
        plainHistory(sheetId, PARENT, parent == null ? "" : parent);
    }

    /** The role the user name acts in on the sheet: a steward while a delegation to them covers the sheet's today,
     *  else the role set for them. */
    public String role(String sheetId, String user) {
        return delegator(sheetId, user) != null ? STEWARD : roleSet(sheetId, user);
    }

    /** The role set for the user name on the sheet; a clerk unless set otherwise. */
    public String roleSet(String sheetId, String user) {
        List<String> found = jdbc.query("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                (rs, i) -> rs.getString(1), sheetId, user == null ? "" : user.trim());
        return found.isEmpty() ? CLERK : found.get(0);
    }

    /** Refuses a change asked for on an archived sheet, or by a user who is a viewer on the sheet. */
    public void refuseViewer(String sheetId, String user) throws ViewerRefusedException {
        if (sheets.archived(sheetId)) {
            throw new net.officefloor.hq.app.sheet.SheetArchivedException();
        }
        if (VIEWER.equals(role(sheetId, user))) {
            throw new ViewerRefusedException();
        }
    }

    public void setRole(String sheetId, String user, String role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                sheetId, user, role);
        plainHistory(sheetId, ROLE + user, role);
    }

    /** The steward whose delegation to the user covers the sheet's today (the latest given, when several do); null for none. */
    public String delegator(String sheetId, String user) {
        List<String> found = jdbc.query("SELECT d.delegated_by FROM sheet_delegation d JOIN sheet s ON s.id = d.sheet_id"
                        + " WHERE d.sheet_id = ? AND d.user_name = ? AND s.today BETWEEN d.from_date AND d.to_date"
                        + " ORDER BY d.seq DESC LIMIT 1",
                (rs, i) -> rs.getString(1), sheetId, user == null ? "" : user.trim());
        return found.isEmpty() ? null : found.get(0);
    }

    public void delegate(String sheetId, String user, LocalDate from, LocalDate to, String by) {
        sheets.today(sheetId);
        jdbc.update("INSERT INTO sheet_delegation (sheet_id, user_name, from_date, to_date, delegated_by, set_on) VALUES (?, ?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))",
                sheetId, user, Date.valueOf(from), Date.valueOf(to), by, sheetId);
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
        plainHistory(sheetId, PERM + action, roles);
    }

    /** The plain_setting_history keys of the undated settings (a permission's and a role's followed by the action or user). */
    public static final String NUMBERING = "numbering", GRACE = "grace", ENTRY = "entry", POT = "pot", PARENT = "parent", FISCAL = "fiscal", ALERT = "alert", ESCALATE = "escalate", REVIEW_SLA = "review-sla", PROOF_KINDS = "proof-kinds", DEFAULT_GRADE = "default-grade", PERM = "perm-", ROLE = "role-";

    /** Records an undated setting's value as set in its history, dated the sheet's today. */
    private void plainHistory(String sheetId, String key, String value) {
        jdbc.update("INSERT INTO plain_setting_history (sheet_id, set_on, setting_key, setting_value) VALUES (?, (SELECT today FROM sheet WHERE id = ?), ?, ?)",
                sheetId, sheetId, key, value);
    }

    /** Whether the user's role may take the action on the sheet: by its permission when set, else by the standing
     *  rule (open, settle, void and close by clerk and steward; reassign and reopen by steward). A viewer takes none,
     *  and no one takes any on an archived sheet. */
    public boolean allows(String sheetId, String user, String action) {
        if (sheets.archived(sheetId)) {
            return false;
        }
        String role = role(sheetId, user);
        if (VIEWER.equals(role)) {
            return false;
        }
        String set = permission(sheetId, action);
        if (set.isEmpty()) {
            return STEWARD.equals(role) || !("reassign".equals(action) || "reopen".equals(action));
        }
        return List.of(set.split(",")).contains(role);
    }

    /** The sheet's settings as shown: each dated one as in force on the sheet's today — the clerk limit ("" for none),
     *  the floor, the terms, the late percent, each grade's own terms (a grade with none then is left out) and the cap
     *  ("" for none) — the proof threshold ("" for none), the policy date ("" for blank), the period numbering from ("" for off), every role set, by user
     *  name, every value recorded for the dated settings and each action's permission ("" for the standing rule), and the grace days ("" for off), and every delegation of the steward role, in the order given. */
    public SettingsResponse load(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        BigDecimal limit = clerkLimit(sheetId);
        BigDecimal cap = cap(sheetId);
        BigDecimal threshold = proofThreshold(sheetId, today);
        BigDecimal noteThreshold = noteThreshold(sheetId, today);
        BigDecimal reviewThreshold = reviewThreshold(sheetId, today);
        InForce rounding = on(sheetId, ValidateSetting.ROUNDING, today);
        LocalDate policy = policyDateSet(sheetId);
        LocalDate numbering = periodNumberingFrom(sheetId);
        Integer grace = graceDays(sheetId);
        Integer escalate = escalateAfter(sheetId);
        Integer reviewSla = reviewSla(sheetId);
        LocalDate entry = entryDate(sheetId);
        BigDecimal pot = pot(sheetId);
        BigDecimal alert = alertThreshold(sheetId);
        String parent = parent(sheetId);
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
                threshold == null ? "" : threshold.setScale(2).toPlainString(), perms,
                grace == null ? "" : String.valueOf(grace), jdbc.query(
                        "SELECT user_name, from_date, to_date, delegated_by FROM sheet_delegation WHERE sheet_id = ? ORDER BY seq",
                        (rs, i) -> new DelegationResponse(rs.getString(1), rs.getDate(2).toLocalDate().toString(),
                                rs.getDate(3).toLocalDate().toString(), rs.getString(4)), sheetId),
                noteThreshold == null ? "" : noteThreshold.setScale(2).toPlainString(), entry == null ? "" : entry.toString(), pot == null ? "" : pot.toPlainString(),
                parent == null ? "" : parent, String.valueOf(fiscalStartMonth(sheetId)),
                alert == null ? "" : alert.toPlainString(), rounding == null ? "" : net.officefloor.hq.app.claim.Rounding.name(rounding.value()),
                reviewThreshold == null ? "" : reviewThreshold.setScale(2).toPlainString(),
                escalate == null ? "" : String.valueOf(escalate), reviewSla == null ? "" : String.valueOf(reviewSla),
                String.join(",", proofKinds(sheetId)), Objects.toString(defaultGradeSet(sheetId), ""));
    }
}

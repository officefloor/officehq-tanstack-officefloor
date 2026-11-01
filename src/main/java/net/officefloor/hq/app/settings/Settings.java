package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
 *  and its dated settings — its clerk limit (and any per grade), its floor, its terms (and any per grade), its late percent
 *  its cap, its proof threshold, its note threshold and its rounding mode — each recorded from from-dates. */
@Repository
public class Settings {

    /** The floor of a sheet that has not set one. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The terms, in days, of a sheet that has not set them. */
    public static final int DEFAULT_TERMS = 14;

    /** The late percent of a sheet that has not set one. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The fiscal year start month of a sheet that has not set one: January. */
    public static final int DEFAULT_FISCAL_START_MONTH = 1;

    /** The keys of the dated settings. */
    public static final String CLERK_LIMIT = "clerk-limit", FLOOR = "floor", TERMS = "terms",
            LATE_PCT = "late-pct", CAP = "cap",
            PROOF_THRESHOLD = "proof-threshold", NOTE_THRESHOLD = "note-threshold", ROUNDING = "rounding",
            REVIEW_THRESHOLD = "review-threshold";

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

    /** The user's role on the sheet in every rule: a steward while a delegation to them covers the
     *  sheet's today, else their own role. */
    public Role role(String sheetId, String user) {
        return delegator(sheetId, user) != null ? Role.STEWARD : ownRole(sheetId, user);
    }

    /** The user's own role on the sheet: a clerk unless given another. */
    public Role ownRole(String sheetId, String user) {
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

    /** Record a delegation of the steward role. */
    public void delegate(Delegation d) {
        jdbc.update("INSERT INTO sheet_delegation (sheet_id, user_name, from_date, to_date, by_user) VALUES (?, ?, ?, ?, ?)",
                d.sheetId(), d.name(), Date.valueOf(d.from()), Date.valueOf(d.to()), d.by());
    }

    /** Every delegation made on the sheet, oldest first. */
    public List<DelegationResponse> delegations(String sheetId) {
        return jdbc.query("SELECT user_name, from_date, to_date, by_user FROM sheet_delegation WHERE sheet_id = ? ORDER BY id",
                (rs, i) -> new DelegationResponse(rs.getString("user_name"), rs.getDate("from_date").toLocalDate().toString(),
                        rs.getDate("to_date").toLocalDate().toString(), rs.getString("by_user")),
                sheetId);
    }

    /** The steward who delegated the steward role to the user for a range covering the sheet's today
     *  (the latest such delegation), or null when none does. */
    public String delegator(String sheetId, String user) {
        if (user == null) return null;
        Date today = Date.valueOf(sheets.today(sheetId));
        return jdbc.query("SELECT by_user FROM sheet_delegation WHERE sheet_id = ? AND user_name = ? "
                + "AND from_date <= ? AND to_date >= ? ORDER BY id DESC LIMIT 1",
                (rs, i) -> rs.getString("by_user"), sheetId, user, today, today)
                .stream().findFirst().orElse(null);
    }

    /** The sheet's clerk limit in force on its today, or null for no limit. */
    public BigDecimal clerkLimit(String sheetId) {
        return valueOn(sheetId, CLERK_LIMIT, sheets.today(sheetId), null);
    }

    /** The clerk limit in force for a claim of `grade`: the grade's own on the sheet's today when one
     *  is set then, else the sheet's. Null for no limit. */
    public BigDecimal clerkLimit(String sheetId, String grade) {
        BigDecimal own = grade != null ? valueOn(sheetId, clerkLimitKey(grade), sheets.today(sheetId), null) : null;
        return own != null ? own : clerkLimit(sheetId);
    }

    /** Every grade's own clerk limit in force on the sheet's today, by grade (a grade not in it uses the sheet's). */
    public Map<String, String> gradeClerkLimit(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        Map<String, String> limits = new TreeMap<>();
        for (String grade : List.of("A", "B", "C")) {
            BigDecimal own = valueOn(sheetId, clerkLimitKey(grade), today, null);
            if (own != null) limits.put(grade, own.toPlainString());
        }
        return limits;
    }

    /** The key a grade's own clerk limit is recorded under. */
    public static String clerkLimitKey(String grade) {
        return CLERK_LIMIT + "-" + grade;
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

    /** The grace days of `grade` in force on `date`: a claim of that grade then is overdue only once
     *  its due-by plus them is before the sheet's today. 0 when none are set then. */
    public int grace(String sheetId, String grade, LocalDate date) {
        BigDecimal days = valueOn(sheetId, graceKey(grade), date, null);
        return days != null ? days.intValue() : 0;
    }

    /** Every grade's grace days in force on the sheet's today, by grade (a grade not in it has none). */
    public Map<String, Integer> gradeGrace(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        Map<String, Integer> grace = new TreeMap<>();
        for (String grade : List.of("A", "B", "C")) {
            BigDecimal days = valueOn(sheetId, graceKey(grade), today, null);
            if (days != null) grace.put(grade, days.intValue());
        }
        return grace;
    }

    /** The key a grade's grace days are recorded under. */
    public static String graceKey(String grade) {
        return "grace-" + grade;
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

    /** The rounding modes a sheet can set, by the name the screen and audit show; stored as a code. */
    private static final Map<String, RoundingMode> ROUNDING_MODES = Map.of("half-up", RoundingMode.HALF_UP,
            "half-even", RoundingMode.HALF_EVEN);

    /** The stored code of a rounding mode's name (1 half-up, 2 half-even); null when not a mode. */
    public static BigDecimal roundingCode(String name) {
        if (name == null) return null;
        return switch (name) {
            case "half-up" -> BigDecimal.ONE;
            case "half-even" -> BigDecimal.valueOf(2);
            default -> null;
        };
    }

    private static String roundingName(BigDecimal code) {
        return code.intValue() == 1 ? "half-up" : "half-even";
    }

    /** The rounding mode in force on `date`; null when none (the policy date decides). */
    public RoundingMode rounding(String sheetId, LocalDate date) {
        BigDecimal code = valueOn(sheetId, ROUNDING, date, null);
        return code == null ? null : ROUNDING_MODES.get(roundingName(code));
    }

    /** The name of the rounding mode in force on `date` (half-up or half-even); null when none. */
    public String roundingName(String sheetId, LocalDate date) {
        BigDecimal code = valueOn(sheetId, ROUNDING, date, null);
        return code == null ? null : roundingName(code);
    }

    /** Every value recorded for the sheet's dated settings, by key, oldest from-date first. */
    public Map<String, List<DatedValue>> dated(String sheetId) {
        Map<String, List<DatedValue>> dated = new TreeMap<>();
        jdbc.query("SELECT setting_key, from_date, setting_value FROM sheet_setting_dated WHERE sheet_id = ? "
                + "ORDER BY setting_key, from_date",
                (RowCallbackHandler) rs -> {
                    BigDecimal value = rs.getBigDecimal("setting_value");
                    String key = rs.getString("setting_key");
                    dated.computeIfAbsent(key, k -> new ArrayList<>())
                            .add(new DatedValue(rs.getDate("from_date").toLocalDate().toString(),
                                    value == null ? null : ROUNDING.equals(key) ? roundingName(value) : value.toPlainString()));
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

    /** The sheet's pot: shared out among the members of each balanced pool. Null when unset (none). */
    public BigDecimal pot(String sheetId) {
        List<BigDecimal> values = jdbc.query("SELECT pot FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getBigDecimal("pot"), sheetId);
        return values.isEmpty() || values.get(0) == null ? null : values.get(0).setScale(2);
    }

    public void setPot(String sheetId, BigDecimal pot) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, pot) KEY (sheet_id) VALUES (?, ?)", sheetId, pot);
    }

    /** The sheet's alert threshold: a person whose due-total crosses above it is alerted. Null when unset (off). */
    public BigDecimal alertThreshold(String sheetId) {
        List<BigDecimal> values = jdbc.query("SELECT alert_threshold FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getBigDecimal("alert_threshold"), sheetId);
        return values.isEmpty() || values.get(0) == null ? null : values.get(0).setScale(2);
    }

    public void setAlertThreshold(String sheetId, BigDecimal threshold) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, alert_threshold) KEY (sheet_id) VALUES (?, ?)", sheetId, threshold);
    }

    /** The sheet's parent sheet: a grade with no rate in force on a date on this sheet takes the
     *  parent's then (see {@link net.officefloor.hq.app.rate.Rates#pct}). Null when unset (none). Not
     *  dated: read from the live sheet, so an as-of view shows it too. */
    public String parent(String sheetId) {
        List<String> values = jdbc.query("SELECT parent_id FROM PUBLIC.sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getString("parent_id"), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }

    public void setParent(String sheetId, String parent) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, parent_id) KEY (sheet_id) VALUES (?, ?)", sheetId, parent);
    }

    /** The sheet's descendants: its children (the sheets naming it as their parent), their children,
     *  and so on, nearest first. Not dated: read from the live sheets. */
    public List<String> descendants(String sheetId) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>();
        seen.add(sheetId);
        java.util.ArrayDeque<String> todo = new java.util.ArrayDeque<>(List.of(sheetId));
        while (!todo.isEmpty()) {
            for (String child : jdbc.query("SELECT sheet_id FROM PUBLIC.sheet_setting WHERE parent_id = ? ORDER BY sheet_id",
                    (rs, i) -> rs.getString("sheet_id"), todo.poll())) {
                if (seen.add(child)) todo.add(child);
            }
        }
        seen.remove(sheetId);
        return List.copyOf(seen);
    }

    /** Whether the sheet `ancestor` is the sheet `sheetId` or one of its parents, any number of levels up. */
    public boolean isAncestor(String ancestor, String sheetId) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String sheet = sheetId; sheet != null && seen.add(sheet); sheet = parent(sheet)) {
            if (sheet.equals(ancestor)) return true;
        }
        return false;
    }

    /** The sheet's entry date: while set, the default date of a new claim in place of the sheet's
     *  today. Null when unset (off). */
    public LocalDate entryDate(String sheetId) {
        List<Date> values = jdbc.query("SELECT entry_date FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getDate("entry_date"), sheetId);
        return values.isEmpty() || values.get(0) == null ? null : values.get(0).toLocalDate();
    }

    public void setEntryDate(String sheetId, LocalDate date) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, entry_date) KEY (sheet_id) VALUES (?, ?)",
                sheetId, date != null ? Date.valueOf(date) : null);
    }

    /** The sheet's today: the day a claim added now is created on. */
    public LocalDate today(String sheetId) {
        return sheets.today(sheetId);
    }

    /** The default date of a new claim: the sheet's entry date while set, else its today. */
    public LocalDate claimDate(String sheetId) {
        LocalDate entry = entryDate(sheetId);
        return entry != null ? entry : sheets.today(sheetId);
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

    /** The sheet's escalation gap: reminding a claim whose previous reminder was at least this many days
     *  before the sheet's today escalates it (once per claim). Null when unset (off). Not dated. */
    public Integer escalateAfter(String sheetId) {
        List<Integer> values = jdbc.query("SELECT escalate_after FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> (Integer) rs.getObject("escalate_after", Integer.class), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }

    public void setEscalateAfter(String sheetId, Integer days) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, escalate_after) KEY (sheet_id) VALUES (?, ?)", sheetId, days);
    }

    /** The sheet's review SLA: a REVIEW claim whose review day (the sheet's today when it went to REVIEW)
     *  plus this many days is before the sheet's today is review-late. Null when unset (none). Not dated. */
    public Integer reviewSla(String sheetId) {
        List<Integer> values = jdbc.query("SELECT review_sla FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> (Integer) rs.getObject("review_sla", Integer.class), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }

    public void setReviewSla(String sheetId, Integer days) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, review_sla) KEY (sheet_id) VALUES (?, ?)", sheetId, days);
    }

    /** The sheet's mid score threshold: while both thresholds are set, a person whose score is at or above it
     *  (and below the high one) is in the MID band. Null when unset. Not dated. */
    public Integer scoreMid(String sheetId) {
        List<Integer> values = jdbc.query("SELECT score_mid FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> (Integer) rs.getObject("score_mid", Integer.class), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }

    public void setScoreMid(String sheetId, Integer score) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, score_mid) KEY (sheet_id) VALUES (?, ?)", sheetId, score);
    }

    /** The sheet's high score threshold: while both thresholds are set, a person whose score is at or above it
     *  is in the HIGH band. Null when unset. Not dated. */
    public Integer scoreHigh(String sheetId) {
        List<Integer> values = jdbc.query("SELECT score_high FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> (Integer) rs.getObject("score_high", Integer.class), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }

    public void setScoreHigh(String sheetId, Integer score) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, score_high) KEY (sheet_id) VALUES (?, ?)", sheetId, score);
    }

    /** The sheet's proof kinds, in the order set: while set, a claim whose due is above its proof threshold
     *  needs a proof of each before it can be settled or approved. Empty when unset. Not dated. */
    public List<String> proofKinds(String sheetId) {
        String kinds = proofKindsValue(sheetId);
        return kinds == null ? List.of() : List.of(kinds.split(","));
    }

    /** The sheet's proof kinds as set, comma separated; null when unset. */
    public String proofKindsValue(String sheetId) {
        List<String> values = jdbc.query("SELECT proof_kinds FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getString("proof_kinds"), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }

    public void setProofKinds(String sheetId, String kinds) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, proof_kinds) KEY (sheet_id) VALUES (?, ?)", sheetId, kinds);
    }

    /** The grade a person added without one is given (by the quick-add bar, or an import line with a
     *  blank grade): A unless set. Not dated. */
    public String defaultGrade(String sheetId) {
        String grade = defaultGradeValue(sheetId);
        return grade == null ? "A" : grade;
    }

    /** The sheet's default grade as set; null when unset. */
    public String defaultGradeValue(String sheetId) {
        List<String> values = jdbc.query("SELECT default_grade FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> rs.getString("default_grade"), sheetId);
        return values.isEmpty() ? null : values.get(0);
    }

    public void setDefaultGrade(String sheetId, String grade) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, default_grade) KEY (sheet_id) VALUES (?, ?)", sheetId, grade);
    }

    /** The month of the year (1 to 12) the sheet's fiscal year starts on: 1 unless set. Not dated. */
    public int fiscalStartMonth(String sheetId) {
        List<Integer> values = jdbc.query("SELECT fiscal_start_month FROM sheet_setting WHERE sheet_id = ?",
                (rs, i) -> (Integer) rs.getObject("fiscal_start_month", Integer.class), sheetId);
        return values.isEmpty() || values.get(0) == null ? DEFAULT_FISCAL_START_MONTH : values.get(0);
    }

    public void setFiscalStartMonth(String sheetId, int month) {
        jdbc.update("MERGE INTO sheet_setting (sheet_id, fiscal_start_month) KEY (sheet_id) VALUES (?, ?)", sheetId, month);
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

    /** The proof threshold in force on `date` for a claim of `grade`: the grade's own when one is set
     *  then, else the sheet's then. Null for none. */
    public BigDecimal proofThreshold(String sheetId, String grade, LocalDate date) {
        BigDecimal own = grade != null ? valueOn(sheetId, proofKey(grade), date, null) : null;
        return own != null ? own : proofThreshold(sheetId, date);
    }

    /** Every grade's own proof threshold in force on the sheet's today, by grade (a grade not in it uses the sheet's). */
    public Map<String, String> gradeProof(String sheetId) {
        LocalDate today = sheets.today(sheetId);
        Map<String, String> proof = new TreeMap<>();
        for (String grade : List.of("A", "B", "C")) {
            BigDecimal own = valueOn(sheetId, proofKey(grade), today, null);
            if (own != null) proof.put(grade, own.toPlainString());
        }
        return proof;
    }

    /** The key a grade's own proof threshold is recorded under. */
    public static String proofKey(String grade) {
        return PROOF_THRESHOLD + "-" + grade;
    }

    /** The sheet's note threshold in force on `date`: a claim dated then whose due is above it
     *  cannot be opened while its note is empty. Null for none. */
    public BigDecimal noteThreshold(String sheetId, LocalDate date) {
        return valueOn(sheetId, NOTE_THRESHOLD, date, null);
    }

    /** The sheet's review threshold in force on `date`: a claim dated then whose due is above it
     *  goes to REVIEW when opened, instead of OPEN. Null for none. */
    public BigDecimal reviewThreshold(String sheetId, LocalDate date) {
        return valueOn(sheetId, REVIEW_THRESHOLD, date, null);
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
     *  viewer may take none, and no one may on an archived sheet. */
    public boolean allows(String sheetId, String action, String user) {
        if (sheets.archived(sheetId)) return false;
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

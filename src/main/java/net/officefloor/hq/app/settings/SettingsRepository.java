package net.officefloor.hq.app.settings;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.HistoryRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingsRepository {
    public static final String CLERK = "clerk";
    public static final String STEWARD = "steward";
    /** A viewer sees everything and changes nothing. */
    public static final String VIEWER = "viewer";
    /** The actions the permission matrix covers, in the order shown. */
    public static final List<String> PERMISSION_ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    private final JdbcTemplate jdbc;
    private final HistoryRepository history;

    public SettingsRepository(JdbcTemplate jdbc, HistoryRepository history) {
        this.jdbc = jdbc;
        this.history = history;
    }

    public boolean sheetExists(String sheetId) {
        Integer known = jdbc.queryForObject("SELECT COUNT(*) FROM sheet WHERE id = ?", Integer.class, sheetId);
        return known != null && known > 0;
    }

    /** Whether a steward has archived the sheet: every change to it is then refused. */
    public boolean archived(String sheetId) {
        List<Boolean> found = jdbc.query("SELECT archived FROM sheet WHERE id = ?", (rs, i) -> rs.getBoolean("archived"), sheetId);
        return !found.isEmpty() && found.get(0);
    }

    /** The sheet's settings and roles, roles by user name: each dated setting as in force on the sheet's
     *  today, and every value recorded for each, by key. */
    public Settings load(String sheetId) {
        List<Role> roles = jdbc.query("SELECT user_name, role FROM sheet_role WHERE sheet_id = ? ORDER BY user_name",
                (rs, i) -> new Role(rs.getString("user_name"), rs.getString("role")), sheetId);
        LocalDate today = today(sheetId);
        BigDecimal limit = clerkLimit(sheetId);
        BigDecimal cap = cap(sheetId);
        LocalDate policy = policyDate(sheetId);
        LocalDate numbering = periodNumberingFrom(sheetId);
        LocalDate entry = entryDate(sheetId);
        BigDecimal pot = pot(sheetId);
        BigDecimal alert = alertThreshold(sheetId);
        return new Settings(limit == null ? null : money(limit), money(floor(sheetId, today)), terms(sheetId, today),
                money(latePct(sheetId, today)), roles, policy == null ? null : policy.toString(),
                numbering == null ? null : numbering.toString(), gradeTerms(sheetId, today),
                cap == null ? null : money(cap), dated(sheetId), permissions(sheetId), graceDays(sheetId),
                delegations(sheetId), entry == null ? null : entry.toString(), holidays(sheetId),
                voidReasons(sheetId), pot == null ? null : money(pot), parent(sheetId), fiscalStartMonth(sheetId),
                alert == null ? null : money(alert));
    }

    /** The sheet's today; the real date when there is no such sheet. */
    public LocalDate today(String sheetId) {
        List<Date> found = jdbc.query("SELECT today FROM sheet WHERE id = ?", (rs, i) -> rs.getDate("today"), sheetId);
        return found.isEmpty() || found.get(0) == null ? LocalDate.now() : found.get(0).toLocalDate();
    }

    /** The value of a dated setting in force on a date: the one recorded with the latest from-date on or
     *  before it (a list holding null when that one is blank); an empty list when none is recorded then. */
    private List<String> on(String sheetId, String key, LocalDate date) {
        List<String> found = new java.util.ArrayList<>();
        jdbc.query("SELECT setting FROM setting_value WHERE sheet_id = ? AND setting_key = ? AND from_date <= ?"
                + " ORDER BY from_date DESC LIMIT 1",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> found.add(rs.getString("setting")),
                sheetId, key, Date.valueOf(date));
        return found;
    }

    /** Record a dated setting's value from a date (null for blank), replacing one recorded from that date. */
    public void record(String sheetId, String key, LocalDate from, String value) {
        jdbc.update("MERGE INTO setting_value (sheet_id, setting_key, from_date, setting) KEY (sheet_id, setting_key, from_date)"
                + " VALUES (?, ?, ?, ?)", sheetId, key, Date.valueOf(from), value);
        history.setting(sheetId, key, from, value);
    }

    /** Every value recorded for each dated setting, by key, each oldest from-date first. */
    public Map<String, List<DatedValue>> dated(String sheetId) {
        Map<String, List<DatedValue>> dated = new LinkedHashMap<>();
        jdbc.query("SELECT setting_key, from_date, setting FROM setting_value WHERE sheet_id = ? ORDER BY setting_key, from_date",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> dated
                        .computeIfAbsent(rs.getString("setting_key"), k -> new java.util.ArrayList<>())
                        .add(new DatedValue(rs.getDate("from_date").toLocalDate().toString(), rs.getString("setting"))),
                sheetId);
        return dated;
    }

    /** The sheet's clerk limit in force on its today; null when it has none. */
    public BigDecimal clerkLimit(String sheetId) {
        List<String> found = on(sheetId, "clerk-limit", today(sheetId));
        return found.isEmpty() || found.get(0) == null ? null : new BigDecimal(found.get(0));
    }

    public void setClerkLimit(String sheetId, LocalDate from, BigDecimal limit) {
        record(sheetId, "clerk-limit", from, limit == null ? null : money(limit));
    }

    /** The sheet's cap in force on its today: the most a person's due-total can be; null when it has none. */
    public BigDecimal cap(String sheetId) {
        List<String> found = on(sheetId, "cap", today(sheetId));
        return found.isEmpty() || found.get(0) == null ? null : new BigDecimal(found.get(0));
    }

    public void setCap(String sheetId, LocalDate from, BigDecimal cap) {
        record(sheetId, "cap", from, cap == null ? null : money(cap));
    }

    /** The sheet's proof threshold in force on a date: a claim of that date whose due is above it cannot be
     *  settled or approved without a proof; null when it has none. */
    public BigDecimal proofThreshold(String sheetId, LocalDate date) {
        List<String> found = on(sheetId, "proof-threshold", date);
        return found.isEmpty() || found.get(0) == null ? null : new BigDecimal(found.get(0));
    }

    public void setProofThreshold(String sheetId, LocalDate from, BigDecimal threshold) {
        record(sheetId, "proof-threshold", from, threshold == null ? null : money(threshold));
    }

    /** A grade's own proof threshold in force on a date; null when it has none then (the sheet's is used). */
    public BigDecimal gradeProofThreshold(String sheetId, String grade, LocalDate date) {
        List<String> found = on(sheetId, "proof-threshold-" + grade, date);
        return found.isEmpty() || found.get(0) == null ? null : new BigDecimal(found.get(0));
    }

    /** Set a grade's own proof threshold from a date; null means none from then (the sheet's is used). */
    public void setGradeProofThreshold(String sheetId, String grade, LocalDate from, BigDecimal threshold) {
        record(sheetId, "proof-threshold-" + grade, from, threshold == null ? null : money(threshold));
    }

    /** The proof threshold in force for a claim of a date whose person had a grade then (null when none): the
     *  grade's own in force on the date when one is set, else the sheet's; null when neither is. */
    public BigDecimal proofThreshold(String sheetId, String grade, LocalDate date) {
        BigDecimal own = grade == null ? null : gradeProofThreshold(sheetId, grade, date);
        return own != null ? own : proofThreshold(sheetId, date);
    }

    /** The sheet's note threshold in force on a date: a claim of that date whose due is above it cannot be
     *  opened with an empty note; null when it has none. */
    public BigDecimal noteThreshold(String sheetId, LocalDate date) {
        List<String> found = on(sheetId, "note-threshold", date);
        return found.isEmpty() || found.get(0) == null ? null : new BigDecimal(found.get(0));
    }

    public void setNoteThreshold(String sheetId, LocalDate from, BigDecimal threshold) {
        record(sheetId, "note-threshold", from, threshold == null ? null : money(threshold));
    }

    /** The sheet's rounding mode in force on a date (half-up or half-even); null when it has none. */
    public String rounding(String sheetId, LocalDate date) {
        List<String> found = on(sheetId, "rounding", date);
        return found.isEmpty() ? null : found.get(0);
    }

    public void setRounding(String sheetId, LocalDate from, String mode) {
        record(sheetId, "rounding", from, mode);
    }

    /** The rounding a rounding mode setting names (half-up or half-even); null for none (blank). */
    public static java.math.RoundingMode roundingMode(String mode) {
        if ("half-up".equals(mode)) return java.math.RoundingMode.HALF_UP;
        if ("half-even".equals(mode)) return java.math.RoundingMode.HALF_EVEN;
        return null;
    }

    /** A due-total held to the cap: at most the cap when there is one. */
    public static BigDecimal capped(BigDecimal total, BigDecimal cap) {
        return cap != null && total.compareTo(cap) > 0 ? cap.setScale(2) : total;
    }

    /** The sheet's floor in force on a date: the least a live due can be; 1.00 before any is recorded. */
    public BigDecimal floor(String sheetId, LocalDate date) {
        List<String> found = on(sheetId, "floor", date);
        return found.isEmpty() || found.get(0) == null ? BigDecimal.ONE.setScale(2) : new BigDecimal(found.get(0)).setScale(2);
    }

    public void setFloor(String sheetId, LocalDate from, BigDecimal floor) {
        record(sheetId, "floor", from, money(floor));
    }

    /** The sheet's terms in force on a date: the business days after a claim's date that it is due by; 14 before
     *  any are recorded. */
    public int terms(String sheetId, LocalDate date) {
        List<String> found = on(sheetId, "terms", date);
        return found.isEmpty() || found.get(0) == null ? 14 : Integer.parseInt(found.get(0));
    }

    public void setTerms(String sheetId, LocalDate from, int terms) {
        record(sheetId, "terms", from, String.valueOf(terms));
    }

    /** A grade's own terms in force on a date; null when it has none then (it uses the sheet's terms). */
    public Integer gradeTerms(String sheetId, String grade, LocalDate date) {
        List<String> found = on(sheetId, "terms-" + grade, date);
        return found.isEmpty() || found.get(0) == null ? null : Integer.valueOf(found.get(0));
    }

    /** Each grade's own terms in force on a date, by grade, in grade order. */
    public Map<String, Integer> gradeTerms(String sheetId, LocalDate date) {
        Map<String, Integer> terms = new LinkedHashMap<>();
        for (String grade : List.of("A", "B", "C")) {
            Integer own = gradeTerms(sheetId, grade, date);
            if (own != null) terms.put(grade, own);
        }
        return terms;
    }

    /** Set a grade's own terms from a date; null leaves the grade using the sheet's terms from then. */
    public void setGradeTerms(String sheetId, String grade, LocalDate from, Integer terms) {
        record(sheetId, "terms-" + grade, from, terms == null ? null : String.valueOf(terms));
    }

    /** A grade's grace days in force on a date: the days past a claim's due-by before it is overdue; null when it
     *  has none then. */
    public Integer gradeGrace(String sheetId, String grade, LocalDate date) {
        List<String> found = on(sheetId, "grace-" + grade, date);
        return found.isEmpty() || found.get(0) == null ? null : Integer.valueOf(found.get(0));
    }

    /** Set a grade's grace days from a date; null means none from then. */
    public void setGradeGrace(String sheetId, String grade, LocalDate from, Integer days) {
        record(sheetId, "grace-" + grade, from, days == null ? null : String.valueOf(days));
    }

    /** The sheet's late percent in force on a date: what an overdue claim's due gains, of its figure, for
     *  every full 30 days it is overdue; 0.00 before any is recorded. */
    public BigDecimal latePct(String sheetId, LocalDate date) {
        List<String> found = on(sheetId, "late-pct", date);
        return found.isEmpty() || found.get(0) == null ? BigDecimal.ZERO.setScale(2) : new BigDecimal(found.get(0)).setScale(2);
    }

    public void setLatePct(String sheetId, LocalDate from, BigDecimal pct) {
        record(sheetId, "late-pct", from, money(pct));
    }

    /** The sheet's policy date; null when blank (the sheet's today is then the policy date). */
    public LocalDate policyDate(String sheetId) {
        List<Date> found = jdbc.query("SELECT policy_date FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate("policy_date"), sheetId);
        return found.isEmpty() || found.get(0) == null ? null : found.get(0).toLocalDate();
    }

    public void setPolicyDate(String sheetId, LocalDate policyDate) {
        jdbc.update("UPDATE sheet SET policy_date = ? WHERE id = ?", policyDate == null ? null : Date.valueOf(policyDate),
                sheetId);
        history.policy(sheetId, policyDate);
    }

    /** The sheet's period numbering from-date; null when blank (period numbering is off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        List<Date> found = jdbc.query("SELECT period_numbering_from FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate("period_numbering_from"), sheetId);
        return found.isEmpty() || found.get(0) == null ? null : found.get(0).toLocalDate();
    }

    public void setPeriodNumberingFrom(String sheetId, LocalDate from) {
        jdbc.update("UPDATE sheet SET period_numbering_from = ? WHERE id = ?", from == null ? null : Date.valueOf(from),
                sheetId);
    }

    /** The sheet's entry date; null when blank (off: a new claim defaults to the sheet's today). */
    public LocalDate entryDate(String sheetId) {
        List<Date> found = jdbc.query("SELECT entry_date FROM sheet WHERE id = ?",
                (rs, i) -> rs.getDate("entry_date"), sheetId);
        return found.isEmpty() || found.get(0) == null ? null : found.get(0).toLocalDate();
    }

    public void setEntryDate(String sheetId, LocalDate date) {
        jdbc.update("UPDATE sheet SET entry_date = ? WHERE id = ?", date == null ? null : Date.valueOf(date), sheetId);
    }

    /** The sheet's pot; null when blank (none). */
    public BigDecimal pot(String sheetId) {
        List<BigDecimal> found = jdbc.query("SELECT pot FROM sheet WHERE id = ?",
                (rs, i) -> rs.getBigDecimal("pot"), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    public void setPot(String sheetId, BigDecimal pot) {
        jdbc.update("UPDATE sheet SET pot = ? WHERE id = ?", pot, sheetId);
    }

    /** The sheet's alert threshold; null when blank (off). */
    public BigDecimal alertThreshold(String sheetId) {
        List<BigDecimal> found = jdbc.query("SELECT alert_threshold FROM sheet WHERE id = ?",
                (rs, i) -> rs.getBigDecimal("alert_threshold"), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    public void setAlertThreshold(String sheetId, BigDecimal threshold) {
        jdbc.update("UPDATE sheet SET alert_threshold = ? WHERE id = ?", threshold, sheetId);
    }

    /** The month (1 to 12) the sheet's fiscal year starts in; 1 unless set. */
    public int fiscalStartMonth(String sheetId) {
        List<Integer> found = jdbc.query("SELECT fiscal_start_month FROM sheet WHERE id = ?",
                (rs, i) -> rs.getInt("fiscal_start_month"), sheetId);
        return found.isEmpty() ? 1 : found.get(0);
    }

    public void setFiscalStartMonth(String sheetId, int month) {
        jdbc.update("UPDATE sheet SET fiscal_start_month = ? WHERE id = ?", month, sheetId);
    }

    /** The sheet's parent sheet id; null when blank (none). Not dated: read from the sheet as it is now, also on a
     *  screen showing the sheet as of a date. */
    public String parent(String sheetId) {
        List<String> found = jdbc.query("SELECT parent_id FROM PUBLIC.sheet WHERE id = ?",
                (rs, i) -> rs.getString("parent_id"), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    public void setParent(String sheetId, String parent) {
        jdbc.update("UPDATE sheet SET parent_id = ? WHERE id = ?", parent, sheetId);
    }

    /** A sheet's ancestors, nearest first: its parent, the parent's parent and so on, stopping at a sheet seen
     *  before. */
    public List<String> lineage(String sheetId) {
        List<String> found = new java.util.ArrayList<>();
        for (String at = parent(sheetId); at != null && !found.contains(at) && !at.equals(sheetId); at = parent(at)) {
            found.add(at);
        }
        return found;
    }

    /** A sheet's descendants: the sheets naming it as their parent, their children and so on, nearer first,
     *  each once and never the sheet itself. */
    public List<String> descendants(String sheetId) {
        List<String> found = new java.util.ArrayList<>();
        java.util.Deque<String> pending = new java.util.ArrayDeque<>(List.of(sheetId));
        while (!pending.isEmpty()) {
            for (String child : jdbc.queryForList("SELECT id FROM sheet WHERE parent_id = ? ORDER BY id", String.class, pending.poll())) {
                if (child.equals(sheetId) || found.contains(child)) continue;
                found.add(child);
                pending.add(child);
            }
        }
        return found;
    }

    /** The sheets whose rates a sheet's grade falls back to, nearest first: its ancestors up to three levels up. */
    public List<String> rateAncestors(String sheetId) {
        List<String> found = lineage(sheetId);
        return found.size() > 3 ? found.subList(0, 3) : found;
    }

    /** The sheet's grace days; null when blank (off). */
    public Integer graceDays(String sheetId) {
        List<Integer> found = jdbc.query("SELECT grace_days FROM sheet WHERE id = ?",
                (rs, i) -> (Integer) rs.getObject("grace_days", Integer.class), sheetId);
        return found.isEmpty() ? null : found.get(0);
    }

    public void setGraceDays(String sheetId, Integer days) {
        jdbc.update("UPDATE sheet SET grace_days = ? WHERE id = ?", days, sheetId);
    }

    /** The role a user name acts in on the sheet: steward while a delegation to them covers the sheet's today,
     *  else their own role. */
    public String role(String sheetId, String user) {
        return delegator(sheetId, user) != null ? STEWARD : ownRole(sheetId, user);
    }

    /** The role a user name holds on the sheet itself: clerk unless set otherwise. */
    public String ownRole(String sheetId, String user) {
        List<String> found = jdbc.query("SELECT role FROM sheet_role WHERE sheet_id = ? AND user_name = ?",
                (rs, i) -> rs.getString("role"), sheetId, user);
        return found.isEmpty() ? CLERK : found.get(0);
    }

    public void setRole(String sheetId, String user, String role) {
        jdbc.update("MERGE INTO sheet_role (sheet_id, user_name, role) KEY (sheet_id, user_name) VALUES (?, ?, ?)",
                sheetId, user, role);
    }

    /** The steward whose delegation to the user covers the sheet's today (the latest made, when several do);
     *  null when none does. */
    public String delegator(String sheetId, String user) {
        List<String> found = jdbc.query("SELECT by_user FROM delegation WHERE sheet_id = ? AND user_name = ?"
                + " AND from_date <= ? AND to_date >= ? ORDER BY id DESC LIMIT 1",
                (rs, i) -> rs.getString("by_user"), sheetId, user, Date.valueOf(today(sheetId)), Date.valueOf(today(sheetId)));
        return found.isEmpty() ? null : found.get(0);
    }

    public void delegate(String sheetId, String user, String by, LocalDate from, LocalDate to) {
        jdbc.update("INSERT INTO delegation (sheet_id, user_name, by_user, from_date, to_date) VALUES (?, ?, ?, ?, ?)",
                sheetId, user, by, Date.valueOf(from), Date.valueOf(to));
    }

    /** The sheet's holidays (yyyy-MM-dd), earliest first. */
    public List<String> holidays(String sheetId) {
        return jdbc.query("SELECT holiday_date FROM holiday WHERE sheet_id = ? ORDER BY holiday_date",
                (rs, i) -> rs.getDate("holiday_date").toLocalDate().toString(), sheetId);
    }

    public void addHoliday(String sheetId, LocalDate date) {
        jdbc.update("INSERT INTO holiday (sheet_id, holiday_date) VALUES (?, ?)", sheetId, Date.valueOf(date));
    }

    /** The sheet's void reasons, in the order they were added. */
    public List<String> voidReasons(String sheetId) {
        return jdbc.query("SELECT reason FROM void_reason WHERE sheet_id = ? ORDER BY position",
                (rs, i) -> rs.getString("reason"), sheetId);
    }

    public void addVoidReason(String sheetId, String reason) {
        jdbc.update("INSERT INTO void_reason (sheet_id, position, reason)"
                + " VALUES (?, (SELECT COALESCE(MAX(position), 0) + 1 FROM void_reason WHERE sheet_id = ?), ?)",
                sheetId, sheetId, reason);
    }

    /** The terms-th business day after a date on the sheet: counting forward from the day after it, skipping
     *  every Saturday, Sunday and holiday on the sheet; the date itself for terms 0. */
    public LocalDate businessDaysAfter(String sheetId, LocalDate date, int terms) {
        return businessDaysAfter(new java.util.HashSet<>(holidays(sheetId)), date, terms);
    }

    /** The terms-th business day after a date, skipping every Saturday, Sunday and one of the holidays (yyyy-MM-dd). */
    public static LocalDate businessDaysAfter(java.util.Set<String> holidays, LocalDate date, int terms) {
        int counted = 0;
        while (counted < terms) {
            date = date.plusDays(1);
            java.time.DayOfWeek day = date.getDayOfWeek();
            if (day != java.time.DayOfWeek.SATURDAY && day != java.time.DayOfWeek.SUNDAY
                    && !holidays.contains(date.toString())) {
                counted++;
            }
        }
        return date;
    }

    /** The delegations made on the sheet, oldest first. */
    public List<Delegation> delegations(String sheetId) {
        return jdbc.query("SELECT user_name, by_user, from_date, to_date FROM delegation WHERE sheet_id = ? ORDER BY id",
                (rs, i) -> new Delegation(rs.getString("user_name"), rs.getString("by_user"),
                        rs.getDate("from_date").toLocalDate().toString(), rs.getDate("to_date").toLocalDate().toString()),
                sheetId);
    }

    /** The permission matrix entries set on the sheet, by action, each a comma separated list of roles; an
     *  action not listed keeps the standing rule. */
    public Map<String, String> permissions(String sheetId) {
        Map<String, String> perms = new LinkedHashMap<>();
        jdbc.query("SELECT action, roles FROM sheet_permission WHERE sheet_id = ?",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> perms.put(rs.getString("action"), rs.getString("roles")),
                sheetId);
        Map<String, String> ordered = new LinkedHashMap<>();
        for (String action : PERMISSION_ACTIONS) {
            if (perms.containsKey(action)) ordered.put(action, perms.get(action));
        }
        return ordered;
    }

    /** Set the roles that may take an action (a comma separated list); null or blank restores the standing rule. */
    public void setPermission(String sheetId, String action, String roles) {
        if (roles == null || roles.isEmpty()) {
            jdbc.update("DELETE FROM sheet_permission WHERE sheet_id = ? AND action = ?", sheetId, action);
        } else {
            jdbc.update("MERGE INTO sheet_permission (sheet_id, action, roles) KEY (sheet_id, action) VALUES (?, ?, ?)",
                    sheetId, action, roles);
        }
    }

    /** Whether a user may take an action on the sheet: by the sheet's permission matrix entry for it, else the
     *  standing rule (open, settle, void and close by clerk and steward; reassign and reopen by steward). */
    public boolean allows(String sheetId, String action, String user) {
        List<String> found = jdbc.query("SELECT roles FROM sheet_permission WHERE sheet_id = ? AND action = ?",
                (rs, i) -> rs.getString("roles"), sheetId, action);
        List<String> roles = found.isEmpty()
                ? (action.equals("reassign") || action.equals("reopen") ? List.of(STEWARD) : List.of(CLERK, STEWARD))
                : List.of(found.get(0).split(","));
        return roles.contains(role(sheetId, user));
    }

    /** A limit as shown: two decimals, a dot, no grouping. */
    public static String money(BigDecimal v) {
        return v.setScale(2).toPlainString();
    }
}

package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.sheet.SheetStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings kept per sheet, by key. A setting never set, or set blank, has no value. The settings
 *  that carry a figure (the clerk limit, a grade's clerk limit, the floor, the cap, the proof threshold, a grade's proof threshold, the note threshold, the review threshold, the terms, a grade's terms, a grade's grace days and the late
 *  percent) are dated: each value is recorded from a from-date, and the value in force on a date is the
 *  one with the latest from-date on or before it (none before every from-date, then the sheet's starting
 *  value). The policy date, the period numbering date, the entry date, the grace days, the escalation gap, the review SLA, the pot, the alert threshold, the parent sheet, the fiscal start month, the default grade, the proof kinds and the permission matrix are not dated. */
@Repository
public class SettingStore {

    public static final String CLERK_LIMIT = "clerk-limit";

    /** The key of a grade's own clerk limit: clerk-limit-A, clerk-limit-B or clerk-limit-C (dated; blank for
     *  none, then the sheet's). */
    public static final String CLERK_LIMIT_PREFIX = "clerk-limit-";

    /** Whether the key is a grade's clerk limit. */
    public static boolean isGradeClerkLimit(String key) {
        return key.startsWith(CLERK_LIMIT_PREFIX) && GRADES.contains(key.substring(CLERK_LIMIT_PREFIX.length()));
    }

    public static final String FLOOR = "floor";

    public static final String CAP = "cap";

    /** Above it a claim's due needs a proof before it is settled or approved. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** The key of a grade's own proof threshold: proof-threshold-A, proof-threshold-B or proof-threshold-C (dated;
     *  blank for none, then the sheet's). */
    public static final String PROOF_THRESHOLD_PREFIX = "proof-threshold-";

    /** Whether the key is a grade's proof threshold. */
    public static boolean isGradeProofThreshold(String key) {
        return key.startsWith(PROOF_THRESHOLD_PREFIX) && GRADES.contains(key.substring(PROOF_THRESHOLD_PREFIX.length()));
    }

    /** The kinds of proof a claim above its proof threshold needs at least one of each of before it is settled
     *  or approved: listed comma separated (receipt, photo); blank for none, not dated. */
    public static final String PROOF_KINDS = "proof-kinds";

    /** The kinds a proof may be; a proof added with no kind is a receipt. */
    public static final List<String> KINDS = List.of("receipt", "photo");

    /** The proof kinds listed, in the order listed; empty when blank (none). */
    public List<String> proofKinds(String sheetId) {
        String value = find(sheetId, PROOF_KINDS);
        return value == null ? List.of() : List.of(value.split(","));
    }

    /** Above it a claim's due needs a note before it is opened. */
    public static final String NOTE_THRESHOLD = "note-threshold";

    /** Above it a claim's due sends it to REVIEW when opened, instead of OPEN (dated; blank for none). */
    public static final String REVIEW_THRESHOLD = "review-threshold";

    public static final String TERMS = "terms";

    /** The key of a grade's own terms: terms-A, terms-B or terms-C. */
    public static final String TERMS_PREFIX = "terms-";

    /** The grades that may have terms of their own. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    public static final String LATE_PCT = "late-pct";

    public static final String POLICY_DATE = "policy-date";

    /** The rounding mode of a claim's live figures, dated: half-up or half-even, blank for none. */
    public static final String ROUNDING = "rounding";

    public static final String ROUNDING_HALF_UP = "half-up";

    public static final String ROUNDING_HALF_EVEN = "half-even";

    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The default date of a new claim in place of the sheet's today; blank for off. */
    public static final String ENTRY_DATE = "entry-date";

    /** The entry date, or null when blank (off). */
    public LocalDate entryDate(String sheetId) {
        String value = find(sheetId, ENTRY_DATE);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The date a new claim takes when none is typed: the entry date while it is set, else the sheet's today. */
    public LocalDate claimDate(String sheetId) {
        LocalDate entry = entryDate(sheetId);
        return entry != null ? entry : today(sheetId);
    }

    /** The pot shared out among a balanced pool's members (two decimals); blank for none, not dated. */
    public static final String POT = "pot";

    /** The pot, or null when blank (none). */
    public BigDecimal pot(String sheetId) {
        String value = find(sheetId, POT);
        return value == null ? null : new BigDecimal(value);
    }

    /** Above it a person's due-total raises an ALERT when a change takes it there (two decimals); blank for off,
     *  not dated. */
    public static final String ALERT_THRESHOLD = "alert-threshold";

    /** The alert threshold, or null when blank (off). */
    public BigDecimal alertThreshold(String sheetId) {
        String value = find(sheetId, ALERT_THRESHOLD);
        return value == null ? null : new BigDecimal(value);
    }

    /** The grade a person added by the quick-add bar, or by an import line with a blank grade, takes: one of
     *  {@link #GRADES}; blank for A, not dated. */
    public static final String DEFAULT_GRADE = "default-grade";

    /** The sheet's default grade: the one set, else A. */
    public String defaultGrade(String sheetId) {
        String value = find(sheetId, DEFAULT_GRADE);
        return value == null ? "A" : value;
    }

    /** The month (1 to 12) a fiscal year starts in; 1 unless set, not dated. */
    public static final String FISCAL_START_MONTH = "fiscal-start-month";

    /** The month a fiscal year starts in: the one set, else 1 (January). */
    public int fiscalStartMonth(String sheetId) {
        String value = find(sheetId, FISCAL_START_MONTH);
        return value == null ? 1 : Integer.parseInt(value);
    }

    /** The sheet whose rates a grade with no rate in force on a date takes (then its parent's, up to three levels
     *  up); blank for none, not dated. */
    public static final String PARENT = "parent";

    /** The parent sheet's id, or null when blank (none). */
    public String parent(String sheetId) {
        return find(sheetId, PARENT);
    }

    /** The sheet's descendants: the sheets naming it as their parent (its children), then their children, and so
     *  on, each once, by id within each level. The parents are those named now (or, as of a date, then). */
    public List<String> descendants(String sheetId) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>(List.of(sheetId));
        List<String> level = List.of(sheetId);
        while (!level.isEmpty()) {
            List<String> next = new java.util.ArrayList<>();
            for (String parent : level) {
                for (String child : jdbc.queryForList("SELECT sheet_id FROM " + undated + " WHERE setting_key = '"
                        + PARENT + "' AND setting_value = ? ORDER BY sheet_id", String.class, parent)) {
                    if (seen.add(child)) {
                        next.add(child);
                    }
                }
            }
            level = next;
        }
        return List.copyOf(seen).subList(1, seen.size());
    }

    /** The other sheets of the sheet's tree: its ancestors (its parent, its parent's parent, and so on), then its
     *  descendants, each once, among the sheets that exist. The parents are those named now. */
    public List<String> tree(String sheetId) {
        java.util.Set<String> seen = new java.util.LinkedHashSet<>(List.of(sheetId));
        for (String at = parent(sheetId); at != null && seen.add(at); at = parent(at)) {
        }
        seen.addAll(descendants(sheetId));
        seen.remove(sheetId);
        return seen.stream().filter(sheets::exists).toList();
    }

    /** Every sheet of the sheet's tree, the sheet among them: the topmost ancestor and all its descendants. The
     *  parents are those named now. */
    public List<String> wholeTree(String sheetId) {
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheetId));
        String root = sheetId;
        for (String at = parent(sheetId); at != null && seen.add(at); at = parent(at)) {
            root = at;
        }
        java.util.Set<String> all = new java.util.LinkedHashSet<>(List.of(root));
        all.addAll(descendants(root));
        all.add(sheetId);
        return List.copyOf(all);
    }

    /** Whether naming the parent for the sheet would make the sheet its own ancestor: the parent is the sheet,
     *  or has it among its ancestors. */
    public boolean cyclic(String sheetId, String parent) {
        java.util.Set<String> seen = new java.util.HashSet<>();
        for (String at = parent; at != null && seen.add(at); at = parent(at)) {
            if (at.equals(sheetId)) {
                return true;
            }
        }
        return false;
    }

    /** The whole days after its to-date an open period is closed by a new today; blank for off. */
    public static final String GRACE_DAYS = "grace-days";

    /** The whole days at least between a claim's previous reminder and the sheet's today for a reminder to
     *  escalate it; blank for off (not dated). */
    public static final String ESCALATE_AFTER = "escalate-after";

    /** The whole days after its review day a claim in REVIEW may stay there before it is late; blank for none
     *  (not dated). */
    public static final String REVIEW_SLA = "review-sla";

    /** The whole days after its pending day a claim in PENDING may stay there before it is late; blank for none
     *  (not dated). */
    public static final String PENDING_SLA = "pending-sla";

    /** The most OPEN or PENDING claims a person may have for another to be opened; blank for none (a whole
     *  number, not dated). */
    public static final String MAX_OPEN = "max-open";

    /** The score at or above which a person's band is MID, while the high one is set too; blank for none
     *  (a whole number, not dated). */
    public static final String SCORE_MID = "score-mid";

    /** The score at or above which a person's band is HIGH, while the mid one is set too; blank for none
     *  (a whole number, not dated). */
    public static final String SCORE_HIGH = "score-high";

    /** Whether moving the sheet's today records each person's score on the day being left: on, or blank for off
     *  (not dated). */
    public static final String SCORE_LOG = "score-log";

    /** The key of a grade's grace days: grace-A, grace-B or grace-C (dated; blank for none). */
    public static final String GRACE_PREFIX = "grace-";

    /** Whether the key is a grade's grace days. */
    public static boolean isGradeGrace(String key) {
        return key.startsWith(GRACE_PREFIX) && GRADES.contains(key.substring(GRACE_PREFIX.length()));
    }

    /** The key of an action's permission: perm-open, perm-settle, and so on. */
    public static final String PERM_PREFIX = "perm-";

    /** The roles that may take each action while its permission is blank (the standing rule). */
    public static final Map<String, List<Role>> STANDING_PERMS = Map.of(
            "open", List.of(Role.CLERK, Role.STEWARD), "settle", List.of(Role.CLERK, Role.STEWARD),
            "void", List.of(Role.CLERK, Role.STEWARD), "close", List.of(Role.CLERK, Role.STEWARD),
            "reassign", List.of(Role.STEWARD), "reopen", List.of(Role.STEWARD),
            "today", List.of(Role.CLERK, Role.STEWARD));

    /** The actions of the permission matrix, in the order the screen shows them. */
    public static final List<String> PERM_ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen", "today");

    /** The late percent a sheet has until one is set. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The terms (days) a sheet has until they are set. */
    public static final int DEFAULT_TERMS = 14;

    /** The floor a sheet has until one is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    private final JdbcTemplate jdbc;

    private final SheetStore sheets;

    /** Where the undated and the dated settings are read from: their tables, or as they stood at the end of
     *  {@link #asOf}. */
    private final String undated;

    private final String dated;

    /** The date that stands in for the sheet's today; null to read the sheet's today. */
    private final LocalDate asOf;

    @Autowired
    public SettingStore(JdbcTemplate jdbc, SheetStore sheets) {
        this(jdbc, sheets, "sheet_setting", "sheet_setting_from", null);
    }

    private SettingStore(JdbcTemplate jdbc, SheetStore sheets, String undated, String dated, LocalDate asOf) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.undated = undated;
        this.dated = dated;
        this.asOf = asOf;
    }

    /** The settings as they stood at the end of the date (see {@link History}), with the date standing in for
     *  the sheet's today, to read only. */
    public SettingStore asOf(LocalDate date) {
        return new SettingStore(jdbc, sheets, History.asOf("sheet_setting", "sheet_id, setting_key", date),
                History.asOf("sheet_setting_from", "sheet_id, setting_key, from_date", date), date);
    }

    private LocalDate today(String sheetId) {
        return asOf != null ? asOf : sheets.today(sheetId);
    }

    /** The day a claim added now takes its terms on: the sheet's today (the date standing in for it when as of a date). */
    public LocalDate termsDay(String sheetId) {
        return today(sheetId);
    }

    /** Whether the key's values are recorded from a from-date. */
    public static boolean isDated(String key) {
        return !POLICY_DATE.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !GRACE_DAYS.equals(key) && !ESCALATE_AFTER.equals(key) && !REVIEW_SLA.equals(key) && !PENDING_SLA.equals(key)
                && !MAX_OPEN.equals(key)
                && !ENTRY_DATE.equals(key) && !POT.equals(key) && !ALERT_THRESHOLD.equals(key) && !PARENT.equals(key) && !FISCAL_START_MONTH.equals(key)
                && !DEFAULT_GRADE.equals(key) && !SCORE_MID.equals(key) && !SCORE_HIGH.equals(key) && !SCORE_LOG.equals(key)
                && !PROOF_KINDS.equals(key) && !key.startsWith(PERM_PREFIX);
    }

    /** Whether the role may take the action (one of {@link #PERM_ACTIONS}): the roles listed in its
     *  permission, else, when blank, the standing rule. A restricted user takes the actions a clerk may. */
    public boolean allows(String sheetId, String action, Role actingAs) {
        Role role = actingAs == Role.RESTRICTED ? Role.CLERK : actingAs;
        if (role == Role.VIEWER) {
            return false;
        }
        String value = find(sheetId, PERM_PREFIX + action);
        if (value == null) {
            return STANDING_PERMS.get(action).contains(role);
        }
        return Arrays.stream(value.split(",")).map(String::trim).anyMatch(r -> r.equals(role.shown()));
    }

    /** The permissions recorded, by action; a blank one is null (the standing rule). */
    public Map<String, String> perms(String sheetId) {
        Map<String, String> perms = new LinkedHashMap<>();
        for (String action : PERM_ACTIONS) {
            perms.put(action, find(sheetId, PERM_PREFIX + action));
        }
        return perms;
    }

    /** The setting's value (a dated one's in force on the sheet's today), or null when it has none. */
    public String find(String sheetId, String key) {
        if (isDated(key)) {
            return findOn(sheetId, key, today(sheetId));
        }
        List<String> found = jdbc.queryForList(
                "SELECT setting_value FROM " + undated + " WHERE sheet_id = ? AND setting_key = ?", String.class,
                sheetId, key);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The dated setting's values recorded, by from-date (a null value is blank). */
    public NavigableMap<LocalDate, String> history(String sheetId, String key) {
        NavigableMap<LocalDate, String> values = new TreeMap<>();
        jdbc.query("SELECT from_date, setting_value FROM " + dated + " WHERE sheet_id = ? AND setting_key = ?",
                rs -> {
                    values.put(rs.getObject("from_date", LocalDate.class), rs.getString("setting_value"));
                }, sheetId, key);
        return values;
    }

    /** The dated setting's value in force on the date, or null when it has none then. */
    public String findOn(String sheetId, String key, LocalDate date) {
        return on(history(sheetId, key), date);
    }

    private static String on(NavigableMap<LocalDate, String> history, LocalDate date) {
        var entry = date == null ? null : history.floorEntry(date);
        return entry == null ? null : entry.getValue();
    }

    /** The most a clerk may settle a claim dated on the date at (the one in force on that date), or null for no limit. */
    public BigDecimal clerkLimit(String sheetId, LocalDate date) {
        String value = findOn(sheetId, CLERK_LIMIT, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The most a clerk may settle a claim dated on the date at for a person of the grade: the grade's own limit in
     *  force on that date when one is set, else the sheet's; null for no limit. */
    public BigDecimal clerkLimit(String sheetId, String grade, LocalDate date) {
        String own = grade == null ? null : findOn(sheetId, CLERK_LIMIT_PREFIX + grade, date);
        return own != null ? new BigDecimal(own) : clerkLimit(sheetId, date);
    }

    /** The most a person's due-total may be, in force on each date (a person's cap is the one on the date of their
     *  latest claim that counts), or null for no cap then. Read once, for many people. */
    public Function<LocalDate, BigDecimal> caps(String sheetId) {
        NavigableMap<LocalDate, String> history = history(sheetId, CAP);
        return date -> {
            String value = on(history, date);
            return value == null ? null : new BigDecimal(value);
        };
    }

    /** The policy settings, versioned together: every change to one of them is a new policy version. */
    public static final List<String> POLICY_KEYS = List.of(ROUNDING, FLOOR, CAP, TERMS, LATE_PCT, CLERK_LIMIT);

    /** The from-dates of the sheet's policy versions, in the order made (null for one with no from-date: it counts
     *  always); as of a date, those made while the sheet's today was on or before it. Their number is the current
     *  version; the version in force on a date is the number of them on or before it. */
    public List<LocalDate> policyFroms(String sheetId) {
        return jdbc.query("SELECT from_date FROM policy_version WHERE sheet_id = ?"
                + (asOf == null ? "" : " AND made_on <= DATE '" + asOf + "'") + " ORDER BY version",
                (rs, n) -> rs.getObject("from_date", LocalDate.class), sheetId);
    }

    /** The change each of the sheet's policy versions made, as key=value (blank for a null value), in the order
     *  made; as of a date, those made while the sheet's today was on or before it. */
    public List<String> policyChanges(String sheetId) {
        return jdbc.query("SELECT setting_key, setting_value FROM policy_version WHERE sheet_id = ?"
                + (asOf == null ? "" : " AND made_on <= DATE '" + asOf + "'") + " ORDER BY version",
                (rs, n) -> rs.getString("setting_key") + "="
                        + (rs.getString("setting_value") == null ? "" : rs.getString("setting_value")), sheetId);
    }

    /** Records a change to a policy setting as the sheet's next policy version. */
    public void addPolicyVersion(String sheetId, String key, String value, LocalDate from) {
        Integer last = jdbc.queryForObject("SELECT MAX(version) FROM policy_version WHERE sheet_id = ?",
                Integer.class, sheetId);
        jdbc.update("INSERT INTO policy_version (sheet_id, version, setting_key, setting_value, from_date, made_on) "
                + "VALUES (?, ?, ?, ?, ?, ?)", sheetId, last == null ? 1 : last + 1, key, value, from,
                sheets.today(sheetId));
    }

    /** The due above which a claim dated on the date needs a proof before it is settled or approved (the
     *  one in force on that date), or null for none. */
    public BigDecimal proofThreshold(String sheetId, LocalDate date) {
        String value = findOn(sheetId, PROOF_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The due above which a claim dated on the date of a person of the grade then needs a proof before it is
     *  settled or approved: the grade's own threshold in force on that date when one is set, else the sheet's;
     *  null for none. */
    public BigDecimal proofThreshold(String sheetId, String grade, LocalDate date) {
        String own = grade == null ? null : findOn(sheetId, PROOF_THRESHOLD_PREFIX + grade, date);
        return own != null ? new BigDecimal(own) : proofThreshold(sheetId, date);
    }

    /** The due above which a claim dated on the date needs a note before it is opened (the one in force on
     *  that date), or null for none. */
    public BigDecimal noteThreshold(String sheetId, LocalDate date) {
        String value = findOn(sheetId, NOTE_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The due above which a claim dated on the date goes to REVIEW when opened (the one in force on that
     *  date), or null for none. */
    public BigDecimal reviewThreshold(String sheetId, LocalDate date) {
        String value = findOn(sheetId, REVIEW_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The least a claim's live due may be, in force on each date: the floor recorded then, else 1.00. Read once, for many claims. */
    public Function<LocalDate, BigDecimal> floors(String sheetId) {
        return amounts(history(sheetId, FLOOR), DEFAULT_FLOOR);
    }

    /** The percent of an overdue claim's figure charged at each 30-day boundary it is overdue past, in force on each
     *  date: the one recorded then, else 0.00. Read once, for many claims. */
    public Function<LocalDate, BigDecimal> latePcts(String sheetId) {
        return amounts(history(sheetId, LATE_PCT), DEFAULT_LATE_PCT);
    }

    private static Function<LocalDate, BigDecimal> amounts(NavigableMap<LocalDate, String> history,
            BigDecimal starting) {
        return date -> {
            String value = on(history, date);
            return value == null ? starting : new BigDecimal(value);
        };
    }

    /** The due-total at most the cap (none when null). */
    public static BigDecimal capped(BigDecimal dueTotal, BigDecimal cap) {
        return cap != null && dueTotal.compareTo(cap) > 0 ? cap.setScale(2) : dueTotal;
    }

    /** The cap a person's due-total is held to: the cap less their cut total, not below 0.00 (none when null). */
    public static BigDecimal capLess(BigDecimal cap, BigDecimal cutTotal) {
        if (cap == null || cutTotal == null) {
            return cap;
        }
        BigDecimal less = cap.subtract(cutTotal);
        return less.signum() < 0 ? BigDecimal.ZERO.setScale(2) : less.setScale(2);
    }

    /** The days after its date a claim is due by, in force on the date: the terms recorded then, else 14. */
    public int terms(String sheetId, LocalDate date) {
        String value = findOn(sheetId, TERMS, date);
        return value == null ? DEFAULT_TERMS : Integer.parseInt(value);
    }

    /** The grade's own terms (days) in force on the date, or null when it has none then (then the sheet's terms). */
    public Integer gradeTerms(String sheetId, String grade, LocalDate date) {
        String value = findOn(sheetId, TERMS_PREFIX + grade, date);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The days after its date a claim of a person of the grade is due by, in force on the date: the
     *  grade's own terms, else the sheet's. */
    public int terms(String sheetId, String grade, LocalDate date) {
        Integer own = grade == null ? null : gradeTerms(sheetId, grade, date);
        return own == null ? terms(sheetId, date) : own;
    }

    /** The grace days of each grade in force on each date (null when it has none then), the whole days after
     *  its due-by a claim of a person of the grade then is not yet overdue. Read once, for many claims. */
    public BiFunction<String, LocalDate, Integer> graces(String sheetId) {
        Map<String, NavigableMap<LocalDate, String>> histories = new LinkedHashMap<>();
        for (String grade : GRADES) {
            histories.put(grade, history(sheetId, GRACE_PREFIX + grade));
        }
        return (grade, date) -> {
            NavigableMap<LocalDate, String> history = histories.get(grade);
            String value = history == null ? null : on(history, date);
            return value == null ? null : Integer.valueOf(value);
        };
    }

    /** The rounding mode in force on each date (half-up or half-even), or null when none is (blank): then a
     *  claim rounds by the policy date. Read once, for many claims. */
    public Function<LocalDate, RoundingMode> roundings(String sheetId) {
        NavigableMap<LocalDate, String> history = history(sheetId, ROUNDING);
        return date -> {
            String value = on(history, date);
            return ROUNDING_HALF_UP.equals(value) ? RoundingMode.HALF_UP
                    : ROUNDING_HALF_EVEN.equals(value) ? RoundingMode.HALF_EVEN : null;
        };
    }

    /** The date the sheet's rounding turns on, or null when blank (then the sheet's today). */
    public LocalDate policyDate(String sheetId) {
        String value = find(sheetId, POLICY_DATE);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The date from which claims opened are numbered per period, or null when blank (off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        String value = find(sheetId, PERIOD_NUMBERING_FROM);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The whole days after its to-date an open period is closed by a new today, or null when blank (off). */
    public Integer graceDays(String sheetId) {
        String value = find(sheetId, GRACE_DAYS);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The whole days at least between a claim's previous reminder and the sheet's today for a reminder to
     *  escalate it, or null when blank (off). */
    public Integer escalateAfter(String sheetId) {
        String value = find(sheetId, ESCALATE_AFTER);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The whole days after its review day a claim in REVIEW may stay there before it is late, or null when
     *  blank (none). */
    public Integer reviewSla(String sheetId) {
        String value = find(sheetId, REVIEW_SLA);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The most OPEN or PENDING claims a person may have for another to be opened, or null when blank (none). */
    public Integer maxOpen(String sheetId) {
        String value = find(sheetId, MAX_OPEN);
        return value == null ? null : Integer.valueOf(value);
    }

    /** Records the setting's value; a dated one's from the date (replacing a value recorded from it). */
    public void set(String sheetId, String key, String value, LocalDate from) {
        if (isDated(key)) {
            jdbc.update("MERGE INTO sheet_setting_from (sheet_id, setting_key, from_date, setting_value) "
                    + "KEY (sheet_id, setting_key, from_date) VALUES (?, ?, ?, ?)", sheetId, key, from, value);
            return;
        }
        jdbc.update("MERGE INTO sheet_setting (sheet_id, setting_key, setting_value) KEY (sheet_id, setting_key) "
                + "VALUES (?, ?, ?)", sheetId, key, value);
    }
}

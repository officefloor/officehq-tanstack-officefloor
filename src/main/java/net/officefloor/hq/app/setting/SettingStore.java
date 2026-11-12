package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;
import java.util.function.Function;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.sheet.SheetStore;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The settings kept per sheet, by key. A setting never set, or set blank, has no value. The settings
 *  that carry a figure (the clerk limit, the floor, the cap, the proof threshold, the terms, a grade's terms and the late
 *  percent) are dated: each value is recorded from a from-date, and the value in force on a date is the
 *  one with the latest from-date on or before it (none before every from-date, then the sheet's starting
 *  value). The policy date, the period numbering date, the grace days and the permission matrix are not dated. */
@Repository
public class SettingStore {

    public static final String CLERK_LIMIT = "clerk-limit";

    public static final String FLOOR = "floor";

    public static final String CAP = "cap";

    /** Above it a claim's due needs a proof before it is settled or approved. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    public static final String TERMS = "terms";

    /** The key of a grade's own terms: terms-A, terms-B or terms-C. */
    public static final String TERMS_PREFIX = "terms-";

    /** The grades that may have terms of their own. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    public static final String LATE_PCT = "late-pct";

    public static final String POLICY_DATE = "policy-date";

    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The whole days after its to-date an open period is closed by a new today; blank for off. */
    public static final String GRACE_DAYS = "grace-days";

    /** The key of an action's permission: perm-open, perm-settle, and so on. */
    public static final String PERM_PREFIX = "perm-";

    /** The roles that may take each action while its permission is blank (the standing rule). */
    public static final Map<String, List<Role>> STANDING_PERMS = Map.of(
            "open", List.of(Role.CLERK, Role.STEWARD), "settle", List.of(Role.CLERK, Role.STEWARD),
            "void", List.of(Role.CLERK, Role.STEWARD), "close", List.of(Role.CLERK, Role.STEWARD),
            "reassign", List.of(Role.STEWARD), "reopen", List.of(Role.STEWARD));

    /** The actions of the permission matrix, in the order the screen shows them. */
    public static final List<String> PERM_ACTIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    /** The late percent a sheet has until one is set. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The terms (days) a sheet has until they are set. */
    public static final int DEFAULT_TERMS = 14;

    /** The floor a sheet has until one is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    private final JdbcTemplate jdbc;

    private final SheetStore sheets;

    public SettingStore(JdbcTemplate jdbc, SheetStore sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** Whether the key's values are recorded from a from-date. */
    public static boolean isDated(String key) {
        return !POLICY_DATE.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !GRACE_DAYS.equals(key)
                && !key.startsWith(PERM_PREFIX);
    }

    /** Whether the role may take the action (one of {@link #PERM_ACTIONS}): the roles listed in its
     *  permission, else, when blank, the standing rule. */
    public boolean allows(String sheetId, String action, Role role) {
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
            return findOn(sheetId, key, sheets.today(sheetId));
        }
        List<String> found = jdbc.queryForList(
                "SELECT setting_value FROM sheet_setting WHERE sheet_id = ? AND setting_key = ?", String.class,
                sheetId, key);
        return found.isEmpty() ? null : found.get(0);
    }

    /** The dated setting's values recorded, by from-date (a null value is blank). */
    public NavigableMap<LocalDate, String> history(String sheetId, String key) {
        NavigableMap<LocalDate, String> values = new TreeMap<>();
        jdbc.query("SELECT from_date, setting_value FROM sheet_setting_from WHERE sheet_id = ? AND setting_key = ?",
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

    /** The most a clerk may settle a claim's due at (the one in force on the sheet's today), or null for no limit. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = find(sheetId, CLERK_LIMIT);
        return value == null ? null : new BigDecimal(value);
    }

    /** The most a person's due-total may be (the one in force on the sheet's today), or null for no cap. */
    public BigDecimal cap(String sheetId) {
        String value = find(sheetId, CAP);
        return value == null ? null : new BigDecimal(value);
    }

    /** The due above which a claim dated on the date needs a proof before it is settled or approved (the
     *  one in force on that date), or null for none. */
    public BigDecimal proofThreshold(String sheetId, LocalDate date) {
        String value = findOn(sheetId, PROOF_THRESHOLD, date);
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

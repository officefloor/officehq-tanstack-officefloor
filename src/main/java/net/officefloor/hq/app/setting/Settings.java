package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import net.officefloor.hq.app.claim.Late;
import net.officefloor.hq.app.sheet.SheetClock;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * The settings kept per sheet, by key; a key with no value set takes its default. A setting that carries a figure is
 * {@link #dated}: it keeps a value per from-date, the one in force on a date being that of the latest from-date on or
 * before it; the others keep one value.
 */
@Repository
public class Settings {

    /** The due a clerk may settle at most (two decimals); blank: no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The most a person's due-total is (two decimals); blank: no cap. */
    public static final String CAP = "cap";

    /** The due above which a claim needs a proof before it is settled or approved (two decimals); blank: none. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** The key of a grade's own proof threshold (two decimals); blank: none, so the grade takes the sheet's. */
    public static String gradeProofThreshold(String grade) {
        return PROOF_THRESHOLD + "-" + grade;
    }

    /** The due above which a claim with an empty note cannot be opened (two decimals); blank: none. */
    public static final String NOTE_THRESHOLD = "note-threshold";

    /** The least a claim's live due is (two decimals); blank: its default. */
    public static final String FLOOR = "floor";

    /** The days after its date by which a claim is due (a whole number); blank: its default. */
    public static final String TERMS = "terms";

    /** The grades that may have terms of their own, each under the key terms-<grade>. */
    public static final List<String> GRADES = List.of("A", "B", "C");

    /** The key of a grade's own terms (a whole number of days); blank: the sheet's terms. */
    public static String gradeTerms(String grade) {
        return TERMS + "-" + grade;
    }

    /** The key of a grade's grace (a whole number of days added to a claim's due-by before it is overdue); blank: none. */
    public static String gradeGrace(String grade) {
        return "grace-" + grade;
    }

    /** The percent of an overdue claim's figure charged for every full 30 days it is overdue (two decimals). */
    public static final String LATE_PCT = "late-pct";

    /** The date before which a claim rounds half up, on or after it half to even (yyyy-MM-dd); blank: the sheet's today. */
    public static final String POLICY_DATE = "policy-date";

    /** The date from which (on the sheet's today) a claim opened is numbered within its period (yyyy-MM-dd); blank: off. */
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The days after its to-date a period stays open; once its to-date plus them is before the sheet's today, a
     *  today change closes it (a whole number); blank: off. */
    public static final String GRACE_DAYS = "grace-days";

    /** The default date of a new claim in place of the sheet's today (yyyy-MM-dd); blank: off. */
    public static final String ENTRY_DATE = "entry-date";

    /** The sum a balanced pool shares out among its members (two decimals, not dated); blank: none. */
    public static final String POT = "pot";

    /** The id of the sheet whose rates this sheet takes where it has none of its own (not dated, and kept apart from
     *  the sheet's own settings, so a sheet shown as of a date keeps it); blank: none. */
    public static final String PARENT = "parent";

    /** The actions whose roles the permission matrix sets, each under the key perm-<action>. */
    public static final List<String> PERMISSIONS = List.of("open", "settle", "void", "reassign", "close", "reopen");

    /** The key of an action's permitted roles (a comma separated list of roles); blank: its standing rule. */
    public static String perm(String action) {
        return "perm-" + action;
    }

    /** The roles that may take the action under the standing rule: reassign and reopen a steward, the rest any. */
    public static String defaultPerm(String action) {
        return "reassign".equals(action) || "reopen".equals(action) ? "steward" : "clerk,steward";
    }

    /** The late percent a sheet starts with. */
    public static final String DEFAULT_LATE_PCT = "0.00";

    /** The terms a sheet starts with. */
    public static final String DEFAULT_TERMS = "14";

    /** The floor a sheet starts with. */
    public static final String DEFAULT_FLOOR = "1.00";

    /** Whether the key is dated: every setting that carries a figure is but the pot; the policy, period numbering and entry dates, the grace days and the parent are not. */
    public static boolean dated(String key) {
        return !POLICY_DATE.equals(key) && !PERIOD_NUMBERING_FROM.equals(key) && !GRACE_DAYS.equals(key)
                && !ENTRY_DATE.equals(key) && !POT.equals(key) && !PARENT.equals(key) && !key.startsWith("perm-");
    }

    /** The from-date an undated setting's one value is kept under. */
    private static final Date UNDATED = Date.valueOf(LocalDate.of(1, 1, 1));

    private final JdbcTemplate jdbc;

    private final SheetClock clock;

    public Settings(JdbcTemplate jdbc, SheetClock clock) {
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** The value set for an undated key, or "" when none is. */
    public String get(String sheet, String key) {
        List<String> found = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet = ? AND setting_key = ?",
                String.class, sheet, key);
        return found.isEmpty() ? "" : found.get(0);
    }

    /** The value of a dated key in force on the date, or "" when none is recorded on or before it. */
    public String on(String sheet, String key, LocalDate date) {
        List<String> found = jdbc.queryForList("SELECT setting_value FROM setting WHERE sheet = ? AND setting_key = ?"
                + " AND from_date <= ? ORDER BY from_date DESC LIMIT 1", String.class, sheet, key, Date.valueOf(date));
        return found.isEmpty() ? "" : found.get(0);
    }

    /** The values recorded for a dated key, by from-date. */
    public List<DatedValue> history(String sheet, String key) {
        return jdbc.query("SELECT from_date, setting_value FROM setting WHERE sheet = ? AND setting_key = ? ORDER BY from_date",
                (rs, i) -> new DatedValue(rs.getDate(1).toLocalDate().toString(), rs.getString(2)), sheet, key);
    }

    /** Sets an undated key's value. */
    public void set(String sheet, String key, String value) {
        set(sheet, key, null, value);
    }

    /** Records the key's value from the from-date (an undated key ignores it), replacing one already recorded from it. */
    public void set(String sheet, String key, LocalDate from, String value) {
        if (PARENT.equals(key)) {
            jdbc.update("DELETE FROM sheet_parent WHERE child = ?", sheet);
            if (!value.isEmpty()) {
                jdbc.update("INSERT INTO sheet_parent (child, parent) VALUES (?, ?)", sheet, value);
            }
            return;
        }
        Date date = dated(key) && from != null ? Date.valueOf(from) : UNDATED;
        jdbc.update("MERGE INTO setting (sheet, setting_key, from_date, setting_value) KEY (sheet, setting_key, from_date)"
                + " VALUES (?, ?, ?, ?)", sheet, key, date, value);
    }

    /** The dated key's figure by date, starting (before every from-date, or where a blank is in force) at the start. */
    private Dated figure(String sheet, String key, BigDecimal start) {
        return figure(history(sheet, key), start);
    }

    /** A dated key's figure by date from its recorded values, starting (before every from-date, or where a blank is in force) at the start. */
    public static Dated figure(List<DatedValue> history, BigDecimal start) {
        TreeMap<LocalDate, BigDecimal> values = new TreeMap<>();
        for (DatedValue v : history) {
            values.put(LocalDate.parse(v.from()), v.value().isEmpty() ? start : new BigDecimal(v.value()));
        }
        return new Dated(start, values);
    }

    /** The sheet's clerk limit in force on its today, or null when there is none. */
    public BigDecimal clerkLimit(String sheet) {
        String limit = on(sheet, CLERK_LIMIT, clock.today(sheet));
        return limit.isEmpty() ? null : new BigDecimal(limit);
    }

    /** The sheet's cap in force on its today: the most a person's due-total is, or null when there is none. */
    public BigDecimal cap(String sheet) {
        String cap = on(sheet, CAP, clock.today(sheet));
        return cap.isEmpty() ? null : new BigDecimal(cap);
    }

    /** The value in force on the date among a dated key's recorded values, or "" when none is recorded on or before it. */
    public static String on(List<DatedValue> history, LocalDate date) {
        String found = "";
        for (DatedValue v : history) {
            if (!LocalDate.parse(v.from()).isAfter(date)) {
                found = v.value();
            }
        }
        return found;
    }

    /** The due-total capped at the cap (none: as it is); a null (n/a) due-total is not capped. */
    public static BigDecimal capped(BigDecimal dueTotal, BigDecimal cap) {
        return dueTotal == null || cap == null || dueTotal.compareTo(cap) <= 0 ? dueTotal : cap;
    }

    /** The sheet's proof threshold in force on the date, or null when there is none. */
    public BigDecimal proofThreshold(String sheet, LocalDate date) {
        String threshold = on(sheet, PROOF_THRESHOLD, date);
        return threshold.isEmpty() ? null : new BigDecimal(threshold);
    }

    /**
     * The proof threshold of the grade in force on the date: its own when it has one then, else the sheet's (a null
     * grade takes the sheet's); null when there is none.
     */
    public BigDecimal proofThreshold(String sheet, String grade, LocalDate date) {
        String own = grade == null ? "" : on(sheet, gradeProofThreshold(grade), date);
        return own.isEmpty() ? proofThreshold(sheet, date) : new BigDecimal(own);
    }

    /** The sheet's note threshold in force on the date, or null when there is none. */
    public BigDecimal noteThreshold(String sheet, LocalDate date) {
        String threshold = on(sheet, NOTE_THRESHOLD, date);
        return threshold.isEmpty() ? null : new BigDecimal(threshold);
    }

    /** The sheet's floor by date: the least a claim's live due is, the one in force on the claim's date. */
    public Dated floor(String sheet) {
        return figure(sheet, FLOOR, new BigDecimal(DEFAULT_FLOOR));
    }

    /** The sheet's terms in force on the date: the days after its date by which a claim is due. */
    public int terms(String sheet, LocalDate date) {
        String terms = on(sheet, TERMS, date);
        return Integer.parseInt(terms.isEmpty() ? DEFAULT_TERMS : terms);
    }

    /**
     * The terms of the grade in force on the date: its own when it has them then, else the sheet's (a null grade takes
     * the sheet's).
     */
    public int terms(String sheet, String grade, LocalDate date) {
        String own = grade == null ? "" : on(sheet, gradeTerms(grade), date);
        return own.isEmpty() ? terms(sheet, date) : Integer.parseInt(own);
    }

    /**
     * The sheet's late percent by date: charged on an overdue claim's figure for every full 30 days it is overdue, the
     * one in force on each 30-day boundary after its due-by.
     */
    public Dated latePct(String sheet) {
        return figure(sheet, LATE_PCT, new BigDecimal(DEFAULT_LATE_PCT));
    }

    /** Each grade's grace as recorded, by from-date. */
    public Map<String, List<DatedValue>> graces(String sheet) {
        Map<String, List<DatedValue>> graces = new LinkedHashMap<>();
        for (String grade : GRADES) {
            graces.put(grade, history(sheet, gradeGrace(grade)));
        }
        return graces;
    }

    /** What makes the sheet's late charges on its today. */
    public Late late(String sheet, LocalDate today) {
        String policy = get(sheet, POLICY_DATE);
        return new Late(latePct(sheet), today, policy.isEmpty() ? today : LocalDate.parse(policy), graces(sheet));
    }

    /** The date from which claims opened are numbered within their period, or null when they are not. */
    public LocalDate periodNumberingFrom(String sheet) {
        String from = get(sheet, PERIOD_NUMBERING_FROM);
        return from.isEmpty() ? null : LocalDate.parse(from);
    }

    /** The sheet's grace days, or null when they are off. */
    public Integer graceDays(String sheet) {
        String days = get(sheet, GRACE_DAYS);
        return days.isEmpty() ? null : Integer.valueOf(days);
    }

    /** The default date of a new claim: the sheet's entry date while it is set, else the sheet's today. */
    public LocalDate entryDay(String sheet) {
        String entry = get(sheet, ENTRY_DATE);
        return entry.isEmpty() ? clock.today(sheet) : LocalDate.parse(entry);
    }

    /** The sheet's pot, or null when there is none. */
    public BigDecimal pot(String sheet) {
        String pot = get(sheet, POT);
        return pot.isEmpty() ? null : new BigDecimal(pot);
    }

    /** The id of the sheet's parent, or "" when it has none. */
    public String parent(String sheet) {
        List<String> found = jdbc.queryForList("SELECT parent FROM sheet_parent WHERE child = ?", String.class, sheet);
        return found.isEmpty() ? "" : found.get(0);
    }

    /** The sheet's children: the sheets naming it as their parent, by id. */
    public List<String> children(String sheet) {
        return jdbc.queryForList("SELECT child FROM sheet_parent WHERE parent = ? ORDER BY child", String.class, sheet);
    }

    /** The sheet then all its descendants (its children, theirs, and so on), each once. */
    public List<String> family(String sheet) {
        java.util.LinkedHashSet<String> found = new java.util.LinkedHashSet<>(List.of(sheet));
        java.util.ArrayDeque<String> left = new java.util.ArrayDeque<>(List.of(sheet));
        while (!left.isEmpty()) {
            for (String child : children(left.pop())) {
                if (found.add(child)) left.add(child);
            }
        }
        return List.copyOf(found);
    }

    /** Every sheet of the sheet's whole tree: its topmost ancestor then all that ancestor's descendants, each once. */
    public List<String> wholeTree(String sheet) {
        java.util.Set<String> seen = new java.util.HashSet<>(List.of(sheet));
        String root = sheet;
        for (String up = parent(root); !up.isEmpty() && seen.add(up); up = parent(up)) {
            root = up;
        }
        return family(root);
    }

    /** The other sheets of the sheet's tree: its ancestors, then its descendants (theirs too), each once. */
    public List<String> tree(String sheet) {
        java.util.LinkedHashSet<String> found = new java.util.LinkedHashSet<>();
        for (String up = parent(sheet); !up.isEmpty() && !up.equals(sheet) && found.add(up); up = parent(up)) {
        }
        found.addAll(family(sheet));
        found.remove(sheet);
        return List.copyOf(found);
    }

    /** Whether the permission matrix lets the role take the action (a blank entry: the standing rule). */
    public boolean permits(String sheet, String action, String role) {
        String roles = get(sheet, perm(action));
        return List.of((roles.isEmpty() ? defaultPerm(action) : roles).split(",")).contains(role);
    }

    /** The settings in force on the sheet's today, with every dated key's recorded values. */
    public SettingsResponse all(String sheet) {
        LocalDate today = clock.today(sheet);
        Map<String, List<DatedValue>> dated = new LinkedHashMap<>();
        for (String key : List.of(CLERK_LIMIT, CAP, FLOOR, TERMS, gradeTerms("A"), gradeTerms("B"), gradeTerms("C"), gradeGrace("A"), gradeGrace("B"), gradeGrace("C"), LATE_PCT, PROOF_THRESHOLD, gradeProofThreshold("A"), gradeProofThreshold("B"), gradeProofThreshold("C"), NOTE_THRESHOLD)) {
            dated.put(key, history(sheet, key));
        }
        Map<String, String> perms = new LinkedHashMap<>();
        for (String action : PERMISSIONS) {
            perms.put(action, get(sheet, perm(action)));
        }
        return new SettingsResponse(on(sheet, CLERK_LIMIT, today), floor(sheet).on(today).toPlainString(),
                Integer.toString(terms(sheet, today)), latePct(sheet).on(today).toPlainString(), get(sheet, POLICY_DATE),
                get(sheet, PERIOD_NUMBERING_FROM), on(sheet, gradeTerms("A"), today), on(sheet, gradeTerms("B"), today),
                on(sheet, gradeTerms("C"), today), on(sheet, CAP, today), on(sheet, PROOF_THRESHOLD, today), perms, dated,
                get(sheet, GRACE_DAYS), on(sheet, NOTE_THRESHOLD, today), get(sheet, ENTRY_DATE),
                on(sheet, gradeGrace("A"), today), on(sheet, gradeGrace("B"), today), on(sheet, gradeGrace("C"), today),
                on(sheet, gradeProofThreshold("A"), today), on(sheet, gradeProofThreshold("B"), today),
                on(sheet, gradeProofThreshold("C"), today), get(sheet, POT), parent(sheet));
    }
}

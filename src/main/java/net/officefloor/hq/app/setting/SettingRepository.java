package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.role.RoleRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/** The stored settings: per sheet and key, its values each from a from-date (one undated value for an undated key). */
@Repository
public class SettingRepository {

    /** The most a clerk may settle a claim for; blank for no limit. */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The prefix of a grade's own clerk limit: clerk-limit-A, -B or -C (blank: none, so the sheet's applies). */
    public static final String CLERK_LIMIT_PREFIX = "clerk-limit-";

    /** The most a person's due-total may be; blank for none. */
    public static final String CAP = "cap";

    /** The due above which a claim needs a proof before it is settled or approved; blank for none. */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** The proof kinds a claim above its proof threshold needs at least one proof of each of before it is settled or
     *  approved: kinds (receipt, photo) separated by commas; blank for none (undated). */
    public static final String PROOF_KINDS = "proof-kinds";

    /** The kinds a proof may have. */
    public static final List<String> KINDS = List.of("receipt", "photo");

    /** The kind a proof has when none is chosen. */
    public static final String DEFAULT_KIND = "receipt";

    /** The prefix of a grade's own proof threshold: proof-threshold-A, -B or -C (blank: none, so the sheet's applies). */
    public static final String PROOF_THRESHOLD_PREFIX = "proof-threshold-";

    /** The due above which a claim cannot be opened while its note is empty; blank for none. */
    public static final String NOTE_THRESHOLD = "note-threshold";

    /** The due above which an opened claim goes to REVIEW instead of OPEN; blank for none. */
    public static final String REVIEW_THRESHOLD = "review-threshold";

    /** The least a claim's live due may be; 1.00 until set. */
    public static final String FLOOR = "floor";

    /** The floor a sheet has until one is set. */
    public static final BigDecimal DEFAULT_FLOOR = new BigDecimal("1.00");

    /** The days a claim has from its date until it is due by; 14 until set. */
    public static final String TERMS = "terms";

    /** The prefix of a grade's own terms: terms-A, terms-B or terms-C; a grade without its own has the sheet's terms. */
    public static final String TERMS_PREFIX = "terms-";

    /** The prefix of a grade's grace: grace-A, grace-B or grace-C, the days added to a claim's due-by before it is
     *  overdue and its late charge counts (blank: none). */
    public static final String GRACE_PREFIX = "grace-";

    /** The terms a sheet has until they are set. */
    public static final int DEFAULT_TERMS = 14;

    /** The percent of an overdue claim's figure added for every full 30 days it is overdue; 0.00 until set. */
    public static final String LATE_PCT = "late-pct";

    /** The late percent a sheet has until one is set. */
    public static final BigDecimal DEFAULT_LATE_PCT = new BigDecimal("0.00");

    /** The date from which a claim's live figures round half to even (before it, half up); blank: the sheet's today. */
    public static final String POLICY_DATE = "policy-date";

    /** The rounding mode a claim of a date takes (dated): half-up or half-even; blank: none (the policy date decides). */
    public static final String ROUNDING = "rounding";

    public static final String HALF_UP = "half-up";

    public static final String HALF_EVEN = "half-even";

    /** The date from which an opened claim is numbered within its period (see ClaimNumbering); blank: off. */
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The default date of a new claim while set (instead of the sheet's today); blank: off (undated). */
    public static final String ENTRY_DATE = "entry-date";

    /** The days after a period's to-date before a change of the sheet's today closes it; blank for off (undated). */
    public static final String GRACE_DAYS = "grace-days";

    /** The escalation gap: reminding a claim whose previous reminder was at least this many days before the sheet's
     *  today escalates it (once per claim); blank for off (undated). */
    public static final String ESCALATE_AFTER = "escalate-after";

    /** The review SLA: a REVIEW claim whose review day (the sheet's today when it went to REVIEW) plus this many days
     *  is before the sheet's today is review-late; blank for none (undated). */
    public static final String REVIEW_SLA = "review-sla";

    /** The pending SLA: a PENDING claim whose pending day (the sheet's today when it went to PENDING) plus this many
     *  days is before the sheet's today is pending-late; blank for none (undated). */
    public static final String PENDING_SLA = "pending-sla";

    /** The sheet's pot, shared out as each balanced pool's cuts (two decimals); blank for none (undated). */
    public static final String POT = "pot";

    /** The sheet's alert threshold: a person whose due-total rises above it is alerted (see Rollup); two decimals,
     *  blank for off (undated). */
    public static final String ALERT_THRESHOLD = "alert-threshold";

    /** The month (1 to 12) the sheet's fiscal year starts in; 1 unless set (undated). */
    public static final String FISCAL_START_MONTH = "fiscal-start-month";

    /** The sheet's score thresholds: a person's band is HIGH at or above score-high, MID at or above score-mid, else LOW;
     *  whole numbers, blank for none (no band while either is blank; undated). */
    public static final String SCORE_MID = "score-mid";

    public static final String SCORE_HIGH = "score-high";

    /** The most OPEN or PENDING claims a person may have; opening one more is refused (see ClaimRules#overMaxOpen); a whole
     *  number, blank for none (undated). */
    public static final String MAX_OPEN = "max-open";

    /** The sheet's score log: on, or blank for off; while on, changing the sheet's today writes SCORE for every person
     *  (see RecordSnapshot) (undated). */
    public static final String SCORE_LOG = "score-log";

    /** The value that turns the score log on. */
    public static final String ON = "on";

    /** The fiscal year start month a sheet has until one is set. */
    public static final int DEFAULT_FISCAL_START_MONTH = 1;

    /** The grade a new person takes from the quick-add bar or a people import line with a blank grade: A, B or C;
     *  blank for A (undated). */
    public static final String DEFAULT_GRADE = "default-grade";

    /** The grade a sheet's new people take until a default grade is set. */
    public static final String STANDING_GRADE = "A";

    /** The sheet's parent sheet: its id, or blank for none (undated). A grade with no rate in force on a date takes
     *  the parent's, then the parent's parent's, up to PARENT_LEVELS levels up. */
    public static final String PARENT = "parent";

    /** How many levels up the parent chain a rate is looked for. */
    public static final int PARENT_LEVELS = 3;

    /** The prefix of an action's permission: perm-open, perm-settle, perm-void, perm-reassign, perm-close,
     *  perm-reopen or perm-today (changing the sheet's today), its value the roles that may take it, comma separated; blank: the standing rule (see PERMISSIONS). */
    public static final String PERM_PREFIX = "perm-";

    /** The actions with a permission, each with the roles that may take it while its permission is blank. */
    public static final Map<String, String> PERMISSIONS = Map.of(
            "open", RoleRepository.CLERK + "," + RoleRepository.STEWARD,
            "settle", RoleRepository.CLERK + "," + RoleRepository.STEWARD,
            "void", RoleRepository.CLERK + "," + RoleRepository.STEWARD,
            "close", RoleRepository.CLERK + "," + RoleRepository.STEWARD,
            "reassign", RoleRepository.STEWARD,
            "reopen", RoleRepository.STEWARD,
            "today", RoleRepository.CLERK + "," + RoleRepository.STEWARD + "," + RoleRepository.RESTRICTED);

    /** The from-date an undated setting is stored under: before every date. */
    private static final LocalDate UNDATED = LocalDate.of(1, 1, 1);

    private final JdbcTemplate jdbc;

    private final SheetRepository sheets;

    public SettingRepository(JdbcTemplate jdbc, SheetRepository sheets) {
        this.jdbc = jdbc;
        this.sheets = sheets;
    }

    /** The sheet's settings: every value recorded, by key then from-date. */
    public List<Setting> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value, from_date FROM setting WHERE sheet_id = ?"
                + " ORDER BY setting_key, from_date", SettingRepository::map, sheetId);
    }

    /** The setting's value in force on the date (the one with the latest from-date on or before it), or null when it
     *  has none: nothing is recorded on or before the date, or the value in force is blank. */
    public String value(String sheetId, String key, LocalDate date) {
        List<Setting> found = inForce(sheetId, key, date);
        return found.isEmpty() ? null : found.get(0).value();
    }

    /** The setting recorded in force on the date: none, or the one. */
    private List<Setting> inForce(String sheetId, String key, LocalDate date) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value, from_date FROM setting WHERE sheet_id = ?"
                + " AND setting_key = ? AND from_date <= ? ORDER BY from_date DESC LIMIT 1", SettingRepository::map,
                sheetId, key, date);
    }

    /** The undated setting's value, or null when it has none. */
    public String value(String sheetId, String key) {
        return value(sheetId, key, UNDATED);
    }

    /** The sheet's clerk limit in force on its today, or null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = value(sheetId, CLERK_LIMIT, sheets.today(sheetId));
        return value == null ? null : new BigDecimal(value);
    }

    /** The clerk limit of a grade in force on the sheet's today: its own when one is set on it, else the sheet's (a
     *  null grade has the sheet's); null when there is none. */
    public BigDecimal clerkLimit(String sheetId, String grade) {
        String value = grade == null ? null : value(sheetId, CLERK_LIMIT_PREFIX + grade, sheets.today(sheetId));
        return value == null ? clerkLimit(sheetId) : new BigDecimal(value);
    }

    /** The sheet's cap in force on its today, or null when there is none. */
    public BigDecimal cap(String sheetId) {
        String value = value(sheetId, CAP, sheets.today(sheetId));
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's proof threshold in force on the date (a claim's date), or null when there is none. */
    public BigDecimal proofThreshold(String sheetId, LocalDate date) {
        String value = value(sheetId, PROOF_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The proof threshold of a grade in force on the date (a claim's date): its own when one is set on it, else the
     *  sheet's (a null grade has the sheet's); null when there is none. */
    public BigDecimal proofThreshold(String sheetId, String grade, LocalDate date) {
        String value = grade == null ? null : value(sheetId, PROOF_THRESHOLD_PREFIX + grade, date);
        return value == null ? proofThreshold(sheetId, date) : new BigDecimal(value);
    }

    /** The sheet's note threshold in force on the date (a claim's date), or null when there is none. */
    public BigDecimal noteThreshold(String sheetId, LocalDate date) {
        String value = value(sheetId, NOTE_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's review threshold in force on the date (a claim's date), or null when there is none. */
    public BigDecimal reviewThreshold(String sheetId, LocalDate date) {
        String value = value(sheetId, REVIEW_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** A due-total held to the cap less the person's cut total (not below 0.00): that when the total is above it (a
     *  null cap holds nothing). */
    public static BigDecimal capped(BigDecimal total, BigDecimal cap, BigDecimal cut) {
        if (cap == null) {
            return total;
        }
        BigDecimal limit = cap.subtract(cut == null ? BigDecimal.ZERO : cut).max(BigDecimal.ZERO).setScale(2);
        return total.compareTo(limit) > 0 ? limit : total;
    }

    /** The sheet's floor in force on the date (a claim's date): the least the claim's live due may be. */
    public BigDecimal floor(String sheetId, LocalDate date) {
        String value = value(sheetId, FLOOR, date);
        return value == null ? DEFAULT_FLOOR : new BigDecimal(value);
    }

    /** The sheet's terms in force on the date: the days a claim of that date has until it is due by. */
    public int terms(String sheetId, LocalDate date) {
        String value = value(sheetId, TERMS, date);
        return value == null ? DEFAULT_TERMS : Integer.parseInt(value);
    }

    /** The terms of a grade in force on the date: its own when one is recorded on or before it, else the sheet's (a
     *  null grade has the sheet's). */
    public int terms(String sheetId, String grade, LocalDate date) {
        String value = grade == null ? null : value(sheetId, TERMS_PREFIX + grade, date);
        return value == null ? terms(sheetId, date) : Integer.parseInt(value);
    }

    /** The terms a claim of the date has: the person's own when they have them (non-null), else their grade's in
     *  force on the date (see above). */
    public int terms(String sheetId, Integer own, String grade, LocalDate date) {
        return own != null ? own : terms(sheetId, grade, date);
    }

    /** The grace of a grade in force on the date (a claim's date): its days, 0 when it has none (a null grade has none). */
    public int grace(String sheetId, String grade, LocalDate date) {
        String value = grade == null ? null : value(sheetId, GRACE_PREFIX + grade, date);
        return value == null ? 0 : Integer.parseInt(value);
    }

    /** The grace a claim of the person dated the date has: that of the person's grade in force on the date (see above). */
    public int graceOf(String sheetId, long personId, LocalDate date) {
        List<String> grades = jdbc.queryForList("SELECT grade FROM person_grade WHERE sheet_id = ? AND person_id = ?"
                + " AND (from_date IS NULL OR from_date <= ?) ORDER BY from_date DESC NULLS LAST LIMIT 1", String.class,
                sheetId, personId, date);
        return grades.isEmpty() ? 0 : grace(sheetId, grades.get(0), date);
    }

    /** The sheet's late percent in force on the date (a 30-day boundary of an overdue claim): what the claim gains of its
     *  figure for that boundary. */
    public BigDecimal latePct(String sheetId, LocalDate date) {
        String value = value(sheetId, LATE_PCT, date);
        return value == null ? DEFAULT_LATE_PCT : new BigDecimal(value);
    }

    /** The sheet's policy date, or null when it is blank (the sheet's today applies). */
    public LocalDate policyDate(String sheetId) {
        String value = value(sheetId, POLICY_DATE);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The sheet's period numbering from-date, or null when it is blank (off). */
    public LocalDate periodNumberingFrom(String sheetId) {
        String value = value(sheetId, PERIOD_NUMBERING_FROM);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The sheet's entry date, or null when it is blank (off). */
    public LocalDate entryDate(String sheetId) {
        String value = value(sheetId, ENTRY_DATE);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The grade a new person takes when none is given: the sheet's default grade, else A. */
    public String defaultGrade(String sheetId) {
        String value = value(sheetId, DEFAULT_GRADE);
        return value == null ? STANDING_GRADE : value;
    }

    /** The date a new claim given no date takes: the sheet's entry date while set, else the sheet's today. */
    public LocalDate claimDate(String sheetId) {
        LocalDate entry = entryDate(sheetId);
        return entry != null ? entry : today(sheetId);
    }

    /** The sheet's today (see SheetRepository#today). */
    public LocalDate today(String sheetId) {
        return sheets.today(sheetId);
    }

    /** The sheet's grace days, or null when it is blank (off). */
    public Integer graceDays(String sheetId) {
        String value = value(sheetId, GRACE_DAYS);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The sheet's escalation gap in days, or null when it is blank (off). */
    public Integer escalateAfter(String sheetId) {
        String value = value(sheetId, ESCALATE_AFTER);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The sheet's maximum of open claims per person, or null when it is blank (none). */
    public Integer maxOpen(String sheetId) {
        String value = value(sheetId, MAX_OPEN);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The sheet's fiscal year start month (1 to 12), 1 unless set. */
    public int fiscalStartMonth(String sheetId) {
        String value = value(sheetId, FISCAL_START_MONTH);
        return value == null ? DEFAULT_FISCAL_START_MONTH : Integer.parseInt(value);
    }

    /** The sheet's proof kinds, in the order set; empty when none. */
    public List<String> proofKinds(String sheetId) {
        String value = value(sheetId, PROOF_KINDS);
        return value == null ? List.of() : List.of(value.split(","));
    }

    /** The sheet's alert threshold, or null when it is blank (off). */
    public BigDecimal alertThreshold(String sheetId) {
        String value = value(sheetId, ALERT_THRESHOLD);
        return value == null ? null : new BigDecimal(value);
    }

    /** Whether the sheet's score log is on. */
    public boolean scoreLog(String sheetId) {
        return ON.equals(value(sheetId, SCORE_LOG));
    }

    /** The sheet's pot, or null when it is blank (none). */
    public BigDecimal pot(String sheetId) {
        String value = value(sheetId, POT);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's pot as it was stored while the sheet's today was on or before the date, or null when it was blank
     *  (none) or not yet stored. */
    public BigDecimal pot(String sheetId, LocalDate madeOn) {
        String value = null;
        for (Setting s : storedBy(sheetId, madeOn)) {
            if (POT.equals(s.key()) && s.from() == null) {
                value = s.value();
            }
        }
        return value == null || value.isBlank() ? null : new BigDecimal(value);
    }

    /** The sheet's parent sheet id, or null when it has none. */
    public String parent(String sheetId) {
        return value(sheetId, PARENT);
    }

    /** The sheet's parent sheet id as it stood once every setting stored while its today was on or before the date
     *  was stored (see storedBy), or null when it had none then. */
    public String parentAsOf(String sheetId, LocalDate madeOn) {
        String value = null;
        for (Setting s : storedBy(sheetId, madeOn)) {
            if (PARENT.equals(s.key())) {
                value = s.value();
            }
        }
        return value == null || value.isBlank() ? null : value.trim();
    }

    /** The sheet's ancestors, nearest first, up to the given number of levels (stopping at a sheet already seen). */
    public List<String> ancestors(String sheetId, int levels) {
        List<String> found = new java.util.ArrayList<>();
        String at = sheetId;
        while (found.size() < levels) {
            at = parent(at);
            if (at == null || at.equals(sheetId) || found.contains(at)) {
                break;
            }
            found.add(at);
        }
        return found;
    }

    /** The sheet's descendants: the sheets naming it as their parent, then theirs, and so on (each once, the sheet
     *  itself never), in the order found. */
    public List<String> descendants(String sheetId) {
        List<String> found = new java.util.ArrayList<>();
        for (int i = -1; i < found.size(); i++) {
            String at = i < 0 ? sheetId : found.get(i);
            for (String child : jdbc.queryForList("SELECT sheet_id FROM setting WHERE setting_key = ? AND from_date = ?"
                    + " AND setting_value = ? ORDER BY sheet_id", String.class, PARENT, UNDATED, at)) {
                if (!child.equals(sheetId) && !found.contains(child)) {
                    found.add(child);
                }
            }
        }
        return found;
    }

    /** The other sheets of the sheet's tree that exist: its topmost ancestor (itself when it has no parent) and all of
     *  that sheet's descendants, the sheet itself left out. */
    public List<String> tree(String sheetId) {
        List<String> up = ancestors(sheetId, Integer.MAX_VALUE);
        String root = up.isEmpty() ? sheetId : up.get(up.size() - 1);
        List<String> found = new java.util.ArrayList<>();
        found.add(root);
        found.addAll(descendants(root));
        found.removeIf((s) -> s.equals(sheetId) || !sheets.exists(s));
        return found;
    }

    /** Whether the sheet has the other as an ancestor, at any level. */
    public boolean hasAncestor(String sheetId, String ancestor) {
        return ancestors(sheetId, Integer.MAX_VALUE).contains(ancestor);
    }

    /** Whether the role may take the action (open, settle, void, reassign, close, reopen or today) on the sheet: the roles its
     *  permission lists, or the standing rule while it is blank. */
    public boolean permits(String sheetId, String action, String role) {
        String value = value(sheetId, PERM_PREFIX + action);
        String roles = value == null ? PERMISSIONS.get(action) : value;
        return Arrays.asList(roles.split(",")).contains(role);
    }

    /** Every setting value stored on the sheet while its today was on or before the date, in the order stored. */
    public List<Setting> storedBy(String sheetId, LocalDate madeOn) {
        return jdbc.query("SELECT sheet_id, setting_key, setting_value, from_date FROM setting_log WHERE sheet_id = ?"
                + " AND made_on <= ? ORDER BY id", SettingRepository::map, sheetId, madeOn);
    }

    /** Stores the setting from its from-date (null: undated), replacing the value recorded from that date, and logs it
     *  with the sheet's today. */
    public Setting save(Setting setting) {
        LocalDate from = setting.from() == null ? UNDATED : setting.from();
        jdbc.update("DELETE FROM setting WHERE sheet_id = ? AND setting_key = ? AND from_date = ?", setting.sheetId(),
                setting.key(), from);
        jdbc.update("INSERT INTO setting (sheet_id, setting_key, setting_value, from_date) VALUES (?, ?, ?, ?)",
                setting.sheetId(), setting.key(), setting.value(), from);
        jdbc.update("INSERT INTO setting_log (sheet_id, setting_key, setting_value, from_date, made_on) VALUES (?, ?, ?, ?, ?)",
                setting.sheetId(), setting.key(), setting.value(), from, sheets.today(setting.sheetId()));
        return setting;
    }

    private static Setting map(ResultSet rs, int n) throws SQLException {
        LocalDate from = rs.getObject("from_date", LocalDate.class);
        return new Setting(rs.getString("sheet_id"), rs.getString("setting_key"), rs.getString("setting_value"),
                UNDATED.equals(from) ? null : from);
    }
}

package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.LongFunction;
import java.util.function.ToIntFunction;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.Dues;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class SettingRepository {

    /** The clerk limit: the largest due a clerk may settle (two decimals; blank: no limit). */
    public static final String CLERK_LIMIT = "clerk-limit";

    /** The proof threshold: a claim whose due is above the one in force on its date cannot be settled
     *  or approved without a proof (two decimals; blank: none). */
    public static final String PROOF_THRESHOLD = "proof-threshold";

    /** A grade's proof threshold ({@code proof-threshold-<grade>}, dated; two decimals; blank: none):
     *  a claim whose person's grade in force on its date has one in force on that date goes by it
     *  instead of the sheet's. */
    public static final String PROOF_THRESHOLD_OF_GRADE = "proof-threshold-";

    /** The note threshold: a claim whose due is above the one in force on its date cannot be opened
     *  with an empty note (two decimals; blank: none). */
    public static final String NOTE_THRESHOLD = "note-threshold";

    /** The review threshold: a claim whose due is above the one in force on its date goes to REVIEW
     *  when opened instead of OPEN (two decimals; blank: none). */
    public static final String REVIEW_THRESHOLD = "review-threshold";

    /** The cap: the most a person's due-total can be (two decimals; blank: no cap). */
    public static final String CAP = "cap";

    /** The pot: the sum a balanced pool's members cut between them (two decimals; blank: none; not dated). */
    public static final String POT = "pot";

    /** The alert threshold: a person whose due-total crosses above it is alerted (two decimals;
     *  blank: off; not dated). */
    public static final String ALERT_THRESHOLD = "alert-threshold";

    /** The floor: the least a claim's live due can be (two decimals). */
    public static final String FLOOR = "floor";

    /** The terms: the number of business days after its date by which a claim is due (a whole number). */
    public static final String TERMS = "terms";

    /** A grade's own terms ({@code terms-<grade>}; blank: the sheet's terms). */
    public static final String TERMS_OF_GRADE = "terms-";

    /** The late percent: the share of an overdue claim's figure it gains for every full 30 days
     *  it is overdue (two decimals). */
    public static final String LATE_PCT = "late-pct";

    /** The policy date (yyyy-MM-dd; blank: the sheet's today): a claim dated before it rounds half
     *  up, one on or after it half to even. */
    public static final String POLICY_DATE = "policy-date";

    /** The rounding mode (dated; half-up or half-even; blank: none): a claim whose date has one in force
     *  rounds its live figures by it instead of by the policy date. */
    public static final String ROUNDING = "rounding";

    /** The period numbering date (yyyy-MM-dd; blank: off): once the sheet's today is on or after it,
     *  a claim opened is numbered within its period. */
    public static final String PERIOD_NUMBERING_FROM = "period-numbering-from";

    /** The entry date (yyyy-MM-dd; blank: off): while set, a new claim with no date of its own takes
     *  it instead of the sheet's today. */
    public static final String ENTRY_DATE = "entry-date";

    /** The grace days (a whole number; blank: off): when the sheet's today changes, an open period
     *  whose to-date plus them is before the new today is closed. */
    public static final String GRACE_DAYS = "grace-days";

    /** The escalation gap (a whole number of days; blank: off; not dated): reminding a claim whose previous
     *  reminder was at least that many days before the sheet's today escalates it, once per claim. */
    public static final String ESCALATE_AFTER = "escalate-after";

    /** The review SLA (a whole number of days; blank: none; not dated): a REVIEW claim whose review day
     *  (the sheet's today when it went to REVIEW) plus them is before the sheet's today is late. */
    public static final String REVIEW_SLA = "review-sla";

    /** A grade's grace days ({@code grace-<grade>}, dated; a whole number; blank: none): a claim whose
     *  person's grade in force on its date has them in force on that date is overdue, and gains its late
     *  charge, counting from its due-by plus them. */
    public static final String GRACE_OF_GRADE = "grace-";

    /** The parent sheet (a sheet id; blank: none; not dated): a grade with no rate in force on a date on
     *  this sheet takes its parent's, then its parent's parent's, up to three levels up. */
    public static final String PARENT = "parent";

    /** The fiscal year start month (1 to 12; not dated): a fiscal year runs from the first of it for
     *  twelve months and is named FY<yyyy> by the calendar year its first day falls in. */
    public static final String FISCAL_START_MONTH = "fiscal-start-month";

    /** The fiscal year start month until it is first set. */
    public static final String DEFAULT_FISCAL_START_MONTH = "1";

    /** The late percent until it is first set. */
    public static final String DEFAULT_LATE_PCT = "0.00";

    /** The terms until they are first set. */
    public static final String DEFAULT_TERMS = "14";

    /** The floor until it is first set. */
    public static final String DEFAULT_FLOOR = "1.00";

    /** Whether a key is dated: every key that carries a figure (the policy date, the period
     *  numbering date, the grace days, the pot, the fiscal year start month and the permission matrix are not). */
    public static boolean isDated(String key) {
        return !POLICY_DATE.equals(key) && !GRACE_DAYS.equals(key) && !ESCALATE_AFTER.equals(key) && !REVIEW_SLA.equals(key) && !PERIOD_NUMBERING_FROM.equals(key)
                && !ENTRY_DATE.equals(key) && !POT.equals(key) && !ALERT_THRESHOLD.equals(key) && !FISCAL_START_MONTH.equals(key) && !PARENT.equals(key) && !Permissions.isKey(key);
    }

    private final JdbcTemplate jdbc;
    private final SheetRepository sheets;
    private final PersonRepository people;

    public SettingRepository(JdbcTemplate jdbc, SheetRepository sheets, PersonRepository people) {
        this.jdbc = jdbc;
        this.sheets = sheets;
        this.people = people;
    }

    /** Every value recorded for the sheet's settings, by key, then by from-date (none first). */
    public List<Setting> list(String sheetId) {
        return jdbc.query("SELECT sheet_id, setting, val, from_date FROM sheet_setting WHERE sheet_id = ? "
                + "ORDER BY setting, from_key", (rs, n) -> row(rs), sheetId);
    }

    private static Setting row(ResultSet rs) throws SQLException {
        LocalDate from = rs.getObject("from_date", LocalDate.class);
        return new Setting(rs.getString("sheet_id"), rs.getString("setting"),
                rs.getString("val") == null ? "" : rs.getString("val"), from == null ? null : from.toString());
    }

    /** The values recorded for a key, with its value before every from-date ({@code start}). */
    public DatedSetting dated(String sheetId, String key, String start) {
        return new DatedSetting(jdbc.query("SELECT sheet_id, setting, val, from_date FROM sheet_setting "
                + "WHERE sheet_id = ? AND setting = ?", (rs, n) -> row(rs), sheetId, key), start);
    }

    /** The values recorded for a key as they stood at the end of the date (each as the last value
     *  recorded while the sheet's today was on or before the date left it), with its value before
     *  every from-date ({@code start}). */
    public DatedSetting datedAsOf(String sheetId, String key, String start, LocalDate asOf) {
        return new DatedSetting(jdbc.query("SELECT sheet_id, setting, val, from_date FROM setting_history h "
                + "WHERE h.sheet_id = ? AND h.setting = ? AND h.id = (SELECT MAX(h2.id) FROM setting_history h2 "
                + "WHERE h2.sheet_id = h.sheet_id AND h2.setting = h.setting AND h2.from_key = h.from_key "
                + "AND (h2.on_date IS NULL OR h2.on_date <= ?))", (rs, n) -> row(rs), sheetId, key, asOf), start);
    }

    /** The sheet's cap in force on the date as it stood then; null when there was none. */
    public BigDecimal capAsOf(String sheetId, LocalDate asOf) {
        String value = datedAsOf(sheetId, CAP, null, asOf).on(asOf.toString());
        return value == null ? null : new BigDecimal(value);
    }

    /** What a claim's late charge and rounding went by at the end of the date, the date standing in
     *  for the sheet's today. */
    public Dues.Late lateAsOf(String sheetId, LocalDate asOf) {
        String policy = datedAsOf(sheetId, POLICY_DATE, null, asOf).on("9999-12-31");
        return new Dues.Late(datedAsOf(sheetId, LATE_PCT, DEFAULT_LATE_PCT, asOf), asOf,
                policy == null ? asOf : LocalDate.parse(policy),
                grace((grade) -> datedAsOf(sheetId, GRACE_OF_GRADE + grade, null, asOf),
                        (id) -> people.findAsOf(sheetId, id, asOf)),
                datedAsOf(sheetId, ROUNDING, null, asOf));
    }

    /** An undated setting's value; null when blank or never set. */
    public String find(String sheetId, String key) {
        return dated(sheetId, key, null).on("9999-12-31");
    }

    /** The sheet's fiscal year start month (1 to 12). */
    public int fiscalStartMonth(String sheetId) {
        String value = find(sheetId, FISCAL_START_MONTH);
        return Integer.parseInt(value == null || value.isEmpty() ? DEFAULT_FISCAL_START_MONTH : value);
    }

    /** The sheet's pot; null when there is none. */
    public BigDecimal pot(String sheetId) {
        String value = find(sheetId, POT);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's alert threshold; null when it is off. */
    public BigDecimal alertThreshold(String sheetId) {
        String value = find(sheetId, ALERT_THRESHOLD);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's clerk limit in force on its today; null when there is none. */
    public BigDecimal clerkLimit(String sheetId) {
        String value = dated(sheetId, CLERK_LIMIT, null).on(sheets.today(sheetId).toString());
        return value == null ? null : new BigDecimal(value);
    }

    /** The proof threshold of a grade in force on a date (yyyy-MM-dd): its own when set, else the
     *  sheet's (a null grade: the sheet's); null when there is none. */
    public BigDecimal proofThreshold(String sheetId, String grade, String date) {
        String value = grade == null ? null : dated(sheetId, PROOF_THRESHOLD_OF_GRADE + grade, null).on(date);
        if (value == null) {
            value = dated(sheetId, PROOF_THRESHOLD, null).on(date);
        }
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's note threshold in force on a date (yyyy-MM-dd); null when there is none. */
    public BigDecimal noteThreshold(String sheetId, String date) {
        String value = dated(sheetId, NOTE_THRESHOLD, null).on(date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's review threshold in force on a date (yyyy-MM-dd); null when there is none. */
    public BigDecimal reviewThreshold(String sheetId, String date) {
        String value = dated(sheetId, REVIEW_THRESHOLD, null).on(date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's cap in force on its today; null when there is none. */
    public BigDecimal cap(String sheetId) {
        String value = dated(sheetId, CAP, null).on(sheets.today(sheetId).toString());
        return value == null ? null : new BigDecimal(value);
    }

    /** The sheet's floor (two decimals) by date: a claim goes by the one in force on its date. */
    public DatedSetting floor(String sheetId) {
        return dated(sheetId, FLOOR, DEFAULT_FLOOR);
    }

    /** The terms (whole days) of a grade in force on a date: its own, else the sheet's (a null
     *  grade: the sheet's). */
    public int terms(String sheetId, String grade, String date) {
        String value = grade == null ? null : dated(sheetId, TERMS_OF_GRADE + grade, null).on(date);
        return Integer.parseInt(value != null ? value : dated(sheetId, TERMS, DEFAULT_TERMS).on(date));
    }

    /** The terms (whole days) of a claim of the person on a date: their own, else those of their
     *  grade in force on it, else the sheet's, each as in force on the date (a null person: the sheet's). */
    public int terms(String sheetId, net.officefloor.hq.app.person.Person person, String date) {
        Integer own = person == null ? null : person.termsOn(date);
        if (own != null) {
            return own;
        }
        return terms(sheetId, person == null ? null : person.gradeOn(date), date);
    }

    /** The sheet's late percent (two decimals) by date: a late charge goes by the one in force on each 30-day boundary. */
    public DatedSetting latePct(String sheetId) {
        return dated(sheetId, LATE_PCT, DEFAULT_LATE_PCT);
    }

    /** The sheet's policy date: the one set, else its {@code today}. */
    public LocalDate policyDate(String sheetId, LocalDate today) {
        String value = find(sheetId, POLICY_DATE);
        return value == null ? today : LocalDate.parse(value);
    }

    /** The sheet's period numbering date; null when off. */
    public LocalDate periodNumberingFrom(String sheetId) {
        String value = find(sheetId, PERIOD_NUMBERING_FROM);
        return value == null ? null : LocalDate.parse(value);
    }

    /** The date a new claim with no date of its own takes: the sheet's entry date when set, else its today. */
    public LocalDate entryDate(String sheetId) {
        String value = find(sheetId, ENTRY_DATE);
        return value == null ? sheets.today(sheetId) : LocalDate.parse(value);
    }

    /** What a claim's late charge and rounding go by on the sheet now. */
    public Dues.Late late(String sheetId, LocalDate today) {
        return new Dues.Late(latePct(sheetId), today, policyDate(sheetId, today),
                grace((grade) -> dated(sheetId, GRACE_OF_GRADE + grade, null), (id) -> people.find(sheetId, id)),
                dated(sheetId, ROUNDING, null));
    }

    /** A claim's grace days: those of its person's grade in force on the claim's date, as in force on
     *  that date (0: none), each grade's and person's looked up once. */
    private static ToIntFunction<Claim> grace(Function<String, DatedSetting> ofGrade, LongFunction<Person> person) {
        Map<String, DatedSetting> graces = new HashMap<>();
        Map<Long, Optional<Person>> persons = new HashMap<>();
        return (claim) -> {
            Person p = persons.computeIfAbsent(claim.getPersonId(), (id) -> Optional.ofNullable(person.apply(id))).orElse(null);
            if (p == null) return 0;
            String grade = p.gradeOn(claim.getDate());
            if (grade == null) return 0;
            String days = graces.computeIfAbsent(grade, ofGrade).on(claim.getDate());
            return days == null ? 0 : Integer.parseInt(days);
        };
    }

    /** Record the setting's value (null: blank) from the from-date (null: from the beginning of time,
     *  for an undated key), replacing the one recorded from the same from-date. */
    public Setting set(String sheetId, String key, String value, LocalDate from, String user) {
        jdbc.update("DELETE FROM sheet_setting WHERE sheet_id = ? AND setting = ? AND from_key = ?",
                sheetId, key, from == null ? LocalDate.of(1, 1, 1) : from);
        jdbc.update("INSERT INTO sheet_setting (sheet_id, setting, val, from_date, set_by, set_at) "
                + "VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)", sheetId, key, value, from, user);
        jdbc.update("INSERT INTO setting_history (sheet_id, setting, val, from_date, on_date) "
                + "VALUES (?, ?, ?, ?, (SELECT today FROM sheet WHERE id = ?))", sheetId, key, value, from, sheetId);
        return new Setting(sheetId, key, value == null ? "" : value, from == null ? null : from.toString());
    }
}

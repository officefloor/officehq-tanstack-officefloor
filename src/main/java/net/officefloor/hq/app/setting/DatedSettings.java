package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import net.officefloor.hq.app.claim.Claim;
import net.officefloor.hq.app.claim.LateCharge;
import net.officefloor.hq.app.person.Person;

/**
 * A sheet's dated settings: for each key (floor, late-pct, terms, terms-&lt;grade&gt;, grace-&lt;grade&gt;, clerk-limit, cap, proof-threshold, proof-threshold-&lt;grade&gt;, note-threshold, rounding) the values
 * recorded, by from-date (a value null: blank). The value in force on a date is the one with the latest from-date on or
 * before it; before every from-date it is the sheet's starting value (floor 1.00, late percent 0.00, terms 14; none
 * for the clerk limit, the cap, the proof threshold, the note threshold, a grade's own terms, a grade's grace days, a grade's proof threshold and the rounding mode).
 */
public record DatedSettings(Map<String, NavigableMap<LocalDate, String>> values) {

    public static final String FLOOR = "floor";
    public static final String LATE_PCT = "late-pct";
    public static final String TERMS = "terms";
    public static final String CLERK_LIMIT = "clerk-limit";
    public static final String CAP = "cap";
    public static final String PROOF_THRESHOLD = "proof-threshold";
    public static final String NOTE_THRESHOLD = "note-threshold";
    public static final String GRACE = "grace";
    public static final String ROUNDING = "rounding";

    /** The rounding modes that can be set, as typed. */
    public static final String HALF_UP = "half-up";
    public static final String HALF_EVEN = "half-even";

    /** The key of a grade's own terms. */
    public static String gradeTerms(String grade) {
        return TERMS + "-" + grade;
    }

    /** The audit field for a from-date typed with a save, " from=<date>", or "" when none was typed (the sheet's today). */
    public static String auditFrom(LocalDate from) {
        return from == null ? "" : " from=" + from;
    }

    /** The date a value saved with this from-date (null: none typed) is recorded from: it, else the sheet's today. */
    public static LocalDate fromOr(LocalDate from, LocalDate today) {
        return from == null ? today : from;
    }

    /** One value recorded from a date. */
    public record FromValue(LocalDate from, String value) {
    }

    /** The value of the key in force on the date (null: blank, or nothing recorded on or before it). */
    private String on(String key, LocalDate date) {
        NavigableMap<LocalDate, String> recorded = values.get(key);
        if (recorded == null || date == null) {
            return null;
        }
        Map.Entry<LocalDate, String> entry = recorded.floorEntry(date);
        return entry == null ? null : entry.getValue();
    }

    /** The floor in force on the date. */
    public BigDecimal floorOn(LocalDate date) {
        String value = on(FLOOR, date);
        return value == null ? SettingRepository.DEFAULT_FLOOR : new BigDecimal(value).setScale(2);
    }

    /** The late percent in force on the date. */
    public BigDecimal latePctOn(LocalDate date) {
        String value = on(LATE_PCT, date);
        return value == null ? SettingRepository.DEFAULT_LATE_PCT : new BigDecimal(value).setScale(2);
    }

    /** What an overdue claim is charged on the given today: the late percent in force on each 30-day boundary. */
    public LateCharge lateCharge(LocalDate today) {
        return new LateCharge(this::latePctOn, today);
    }

    /**
     * What an overdue claim of the person (null: none, no grace) is charged on the given today: the late percent in
     * force on each 30-day boundary, counted from its due-by plus the grace of the person's grade on its date.
     */
    public LateCharge lateCharge(LocalDate today, Person person) {
        return person == null ? lateCharge(today) : new LateCharge(this::latePctOn, (claim) -> graceFor(person, claim), today);
    }

    /** The key of a grade's grace days. */
    public static String gradeGrace(String grade) {
        return GRACE + "-" + grade;
    }

    /** The grade's grace days in force on the date (null: none). */
    public Integer graceOn(String grade, LocalDate date) {
        String value = grade == null ? null : on(gradeGrace(grade), date);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The grace days for the person's claim: those of the person's grade in force on its date, in force then; else 0. */
    public int graceFor(Person person, Claim claim) {
        Integer grace = graceOn(person.gradeOn(claim.date()), claim.date());
        return grace == null ? 0 : grace;
    }

    /** The sheet's terms in days in force on the date. */
    public int termsOn(LocalDate date) {
        String value = on(TERMS, date);
        return value == null ? SettingRepository.DEFAULT_TERMS : Integer.parseInt(value);
    }

    /** The grade's own terms in force on the date (null: none, the sheet's terms). */
    public Integer gradeTermsOn(String grade, LocalDate date) {
        String value = on(gradeTerms(grade), date);
        return value == null ? null : Integer.valueOf(value);
    }

    /** The terms in days for a claim of the grade dated on the date: the grade's own in force then, else the sheet's. */
    public int termsFor(String grade, LocalDate date) {
        Integer own = grade == null ? null : gradeTermsOn(grade, date);
        return own != null ? own : termsOn(date);
    }

    /** The grades' own terms in force on the date, by grade (a grade with none left out). */
    public Map<String, Integer> gradeTermsOn(LocalDate date) {
        Map<String, Integer> terms = new LinkedHashMap<>();
        values.keySet().stream().filter((k) -> k.startsWith(TERMS + "-")).sorted().forEach((key) -> {
            String grade = key.substring(TERMS.length() + 1);
            Integer own = gradeTermsOn(grade, date);
            if (own != null) {
                terms.put(grade, own);
            }
        });
        return terms;
    }

    /** The clerk limit in force on the date (null: no limit). */
    public BigDecimal clerkLimitOn(LocalDate date) {
        String value = on(CLERK_LIMIT, date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /** The cap in force on the date (null: no cap). */
    public BigDecimal capOn(LocalDate date) {
        String value = on(CAP, date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /** The proof threshold in force on the date (null: none). */
    public BigDecimal proofThresholdOn(LocalDate date) {
        String value = on(PROOF_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /** The key of a grade's own proof threshold. */
    public static String gradeProofThreshold(String grade) {
        return PROOF_THRESHOLD + "-" + grade;
    }

    /** The grade's own proof threshold in force on the date (null: none, the sheet's). */
    public BigDecimal gradeProofThresholdOn(String grade, LocalDate date) {
        String value = grade == null ? null : on(gradeProofThreshold(grade), date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /** The proof threshold for a claim of the grade dated on the date: the grade's own in force then, else the sheet's (null: none). */
    public BigDecimal proofThresholdFor(String grade, LocalDate date) {
        BigDecimal own = gradeProofThresholdOn(grade, date);
        return own != null ? own : proofThresholdOn(date);
    }

    /**
     * Whether a claim of the date, of a person of the grade (null: none), with this due (null: n/a) needs a proof to be
     * settled or approved: its due is above the proof threshold for its grade in force on its date.
     */
    public boolean needsProof(String grade, LocalDate date, BigDecimal due) {
        BigDecimal threshold = proofThresholdFor(grade, date);
        return threshold != null && due != null && due.compareTo(threshold) > 0;
    }

    /** The note threshold in force on the date (null: none). */
    public BigDecimal noteThresholdOn(LocalDate date) {
        String value = on(NOTE_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /**
     * Whether a claim of the date with this due (null: n/a) needs a note to be opened: its due is above the note
     * threshold in force on its date.
     */
    public boolean needsNote(LocalDate date, BigDecimal due) {
        BigDecimal threshold = noteThresholdOn(date);
        return threshold != null && due != null && due.compareTo(threshold) > 0;
    }

    /** The rounding mode in force on the date, as typed (null: none). */
    public String roundingOn(LocalDate date) {
        return on(ROUNDING, date);
    }

    /** The rounding mode in force on the date (null: none, the policy date decides). */
    public RoundingMode roundingModeOn(LocalDate date) {
        String value = roundingOn(date);
        if (HALF_UP.equals(value)) {
            return RoundingMode.HALF_UP;
        }
        return HALF_EVEN.equals(value) ? RoundingMode.HALF_EVEN : null;
    }

    /** Every value recorded, by key, oldest from-date first. */
    public Map<String, List<FromValue>> history() {
        Map<String, List<FromValue>> history = new LinkedHashMap<>();
        values.forEach((key, recorded) -> history.put(key,
                recorded.entrySet().stream().map((e) -> new FromValue(e.getKey(), e.getValue())).toList()));
        return history;
    }
}

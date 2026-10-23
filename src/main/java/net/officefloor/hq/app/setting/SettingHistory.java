package net.officefloor.hq.app.setting;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.TreeMap;

/** A sheet's dated settings: by key (floor, late-pct, terms, terms-A/B/C, grace-A/B/C, clerk-limit, cap,
 *  proof-threshold, note-threshold, proof-threshold-A/B/C, rounding, review-threshold), each value recorded
 *  by its from-date (null: blank). The value in force on a date is the one with the latest from-date on or before
 *  it; before every from-date the sheet's starting value applies. */
public record SettingHistory(Map<String, NavigableMap<LocalDate, String>> values) {

    /** The keys of the settings that are dated. */
    public static final List<String> KEYS = List.of("clerk-limit", "floor", "terms", "terms-A",
            "terms-B", "terms-C", "grace-A", "grace-B", "grace-C", "late-pct", "cap", "proof-threshold",
            "note-threshold", "proof-threshold-A", "proof-threshold-B", "proof-threshold-C", "rounding",
            "review-threshold");

    /** The key of the rounding mode: half-up or half-even (blank: none, the policy date rounds). */
    public static final String ROUNDING = "rounding";

    /** The rounding modes. */
    public static final String HALF_UP = "half-up";
    public static final String HALF_EVEN = "half-even";

    /** The rounding mode in force on the date: half-up or half-even, or null when there is none. */
    public String roundingOn(LocalDate date) {
        var e = entryOn(ROUNDING, date);
        return e != null ? e.getValue() : null;
    }

    /** The key's values, oldest from-date first (empty when it has none). */
    public NavigableMap<LocalDate, String> of(String key) {
        return values.getOrDefault(key, new TreeMap<>());
    }

    /** The key's value in force on the date (null date: its latest); null when none is (its starting value applies). */
    private Map.Entry<LocalDate, String> entryOn(String key, LocalDate date) {
        NavigableMap<LocalDate, String> theirs = of(key);
        return date == null ? theirs.lastEntry() : theirs.floorEntry(date);
    }

    /** The floor in force on the date: two decimals. */
    public BigDecimal floorOn(LocalDate date) {
        var e = entryOn("floor", date);
        return e != null && e.getValue() != null ? new BigDecimal(e.getValue()).setScale(2) : SettingRepository.DEFAULT_FLOOR;
    }

    /** The late percent in force on the date: two decimals. */
    public BigDecimal latePctOn(LocalDate date) {
        var e = entryOn("late-pct", date);
        return e != null && e.getValue() != null ? new BigDecimal(e.getValue()).setScale(2) : SettingRepository.DEFAULT_LATE_PCT;
    }

    /** The sheet's terms in force on the date, in days. */
    public int termsOn(LocalDate date) {
        var e = entryOn("terms", date);
        return e != null && e.getValue() != null ? Integer.parseInt(e.getValue()) : SettingRepository.DEFAULT_TERMS;
    }

    /** The grade's own terms in force on the date, in days; null when it has none then (it uses the sheet's). */
    public Integer gradeTermsOn(String grade, LocalDate date) {
        var e = entryOn("terms-" + grade, date);
        return e != null && e.getValue() != null ? Integer.valueOf(e.getValue()) : null;
    }

    /** The terms a claim of the grade dated on the date gets: the grade's own then, else the sheet's then. */
    public int termsFor(String grade, LocalDate date) {
        Integer own = grade != null ? gradeTermsOn(grade, date) : null;
        return own != null ? own : termsOn(date);
    }

    /** The grade's grace in force on the date, in days; null when it has none then. */
    public Integer graceOn(String grade, LocalDate date) {
        var e = entryOn("grace-" + grade, date);
        return e != null && e.getValue() != null ? Integer.valueOf(e.getValue()) : null;
    }

    /** The grace a claim of the grade dated on the date gets, in days: the grade's in force then, else none (0). */
    public int graceFor(String grade, LocalDate date) {
        Integer own = grade != null && !grade.isEmpty() ? graceOn(grade, date) : null;
        return own != null ? own : 0;
    }

    /** The clerk limit in force on the date: two decimals, or null when there is none. */
    public BigDecimal clerkLimitOn(LocalDate date) {
        return amountOn("clerk-limit", date);
    }

    /** The cap in force on the date: two decimals, or null when there is none. */
    public BigDecimal capOn(LocalDate date) {
        return amountOn("cap", date);
    }

    /** The proof threshold in force on the date: two decimals, or null when there is none. */
    public BigDecimal proofThresholdOn(LocalDate date) {
        return amountOn("proof-threshold", date);
    }

    /** The grade's own proof threshold in force on the date: two decimals, or null when it has none then (it uses
     *  the sheet's). */
    public BigDecimal gradeProofThresholdOn(String grade, LocalDate date) {
        return amountOn("proof-threshold-" + grade, date);
    }

    /** The proof threshold a claim of the grade dated on the date has: the grade's own in force then when it has
     *  one, else the sheet's then; null when there is none. */
    public BigDecimal proofThresholdFor(String grade, LocalDate date) {
        BigDecimal own = grade != null && !grade.isEmpty() ? gradeProofThresholdOn(grade, date) : null;
        return own != null ? own : proofThresholdOn(date);
    }

    /** The note threshold in force on the date: two decimals, or null when there is none. */
    public BigDecimal noteThresholdOn(LocalDate date) {
        return amountOn("note-threshold", date);
    }

    /** The review threshold in force on the date: two decimals, or null when there is none. */
    public BigDecimal reviewThresholdOn(LocalDate date) {
        return amountOn("review-threshold", date);
    }

    private BigDecimal amountOn(String key, LocalDate date) {
        var e = entryOn(key, date);
        return e != null && e.getValue() != null ? new BigDecimal(e.getValue()).setScale(2) : null;
    }
}

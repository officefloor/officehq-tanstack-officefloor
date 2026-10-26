package net.officefloor.hq.app.access;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.person.Person;

/**
 * A sheet's dated settings: each key's value in force on a date is the one with the latest from-date on or before it;
 * before every from-date, the key's starting value (see {@link Setting#starting}).
 */
public record DatedSettings(List<DatedValue> values) {

    /** The key's value in force on the date; null when blank. */
    public String on(String key, LocalDate date) {
        DatedValue found = null;
        for (DatedValue v : values) {
            if (v.key().equals(key) && !v.from().isAfter(date) && (found == null || v.from().isAfter(found.from()))) {
                found = v;
            }
        }
        return found == null ? Setting.starting(key) : found.value();
    }

    /** The floor in force on the date: the least a claim's live due may be. */
    public BigDecimal floor(LocalDate date) {
        return new BigDecimal(orStarting(Setting.FLOOR, date)).setScale(2);
    }

    /** The rounding mode in force on the date; null when there is none (the policy date decides). */
    public java.math.RoundingMode rounding(LocalDate date) {
        String value = on(Setting.ROUNDING, date);
        if (Setting.ROUNDING_HALF_UP.equals(value)) {
            return java.math.RoundingMode.HALF_UP;
        }
        return Setting.ROUNDING_HALF_EVEN.equals(value) ? java.math.RoundingMode.HALF_EVEN : null;
    }

    /** The late percent in force on the date. */
    public BigDecimal latePct(LocalDate date) {
        return new BigDecimal(orStarting(Setting.LATE_PCT, date)).setScale(2);
    }

    /** The terms of the grade in force on the date: its own (setting terms-<grade>), else the sheet's. */
    public int terms(String grade, LocalDate date) {
        String value = grade == null ? null : on(Setting.TERMS_OF_GRADE + grade, date);
        return Integer.parseInt(value == null ? orStarting(Setting.TERMS, date) : value);
    }

    /** The terms in force on the date for the person: their own in force on that date, else their grade's on that date, else the sheet's. */
    public int terms(Person person, LocalDate date) {
        Integer own = person == null ? null : person.termsOn(date);
        if (own != null) {
            return own;
        }
        return terms(person == null ? null : person.gradeOn(date), date);
    }

    /** The grace of the grade in force on the date (setting grace-<grade>): the days past its due-by a claim goes overdue; 0 while blank. */
    public int grace(String grade, LocalDate date) {
        String value = grade == null ? null : on(Setting.GRACE_OF_GRADE + grade, date);
        return value == null ? 0 : Integer.parseInt(value);
    }

    /** The clerk limit in force on the date; null when there is none. */
    public BigDecimal clerkLimit(LocalDate date) {
        String value = on(Setting.CLERK_LIMIT, date);
        return value == null ? null : new BigDecimal(value);
    }

    /** The proof threshold in force on the date; null when there is none. */
    public BigDecimal proofThreshold(LocalDate date) {
        String value = on(Setting.PROOF_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /** The proof threshold in force on the date for a claim of the grade: the grade's own (setting proof-threshold-<grade>) when set, else the sheet's; null when there is none. */
    public BigDecimal proofThreshold(String grade, LocalDate date) {
        String value = grade == null ? null : on(Setting.PROOF_THRESHOLD_OF_GRADE + grade, date);
        return value == null ? proofThreshold(date) : new BigDecimal(value).setScale(2);
    }

    /** The note threshold in force on the date; null when there is none. */
    public BigDecimal noteThreshold(LocalDate date) {
        String value = on(Setting.NOTE_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /** The review threshold in force on the date; null when there is none. */
    public BigDecimal reviewThreshold(LocalDate date) {
        String value = on(Setting.REVIEW_THRESHOLD, date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    /** The cap in force on the date: the most a person's due-total may be; null when there is none. */
    public BigDecimal cap(LocalDate date) {
        String value = on(Setting.CAP, date);
        return value == null ? null : new BigDecimal(value).setScale(2);
    }

    private String orStarting(String key, LocalDate date) {
        String value = on(key, date);
        return value == null ? Setting.starting(key) : value;
    }
}

package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * A grade's rate on a sheet: a percent with two decimals, as the screen shows it, in force from its from-date
 * (yyyy-MM-dd; null when it applies from the beginning of time) through its to-date (null when it has no end). Its
 * level is the sheet it is kept on: 0 this sheet's own, 1 its parent's, 2 its parent's parent, up to {@link #LEVELS};
 * a cloned sheet then has the rates of the sheet it was cloned from (and so on), each {@link #SOURCE_LEVELS} further up.
 * Its version counts the values set for its grade and from-date, this one (the newest, the one in force) included.
 * A forward-only rate (saved with a from-date on or after the sheet's today) carries the claim sequence's value when
 * it was saved (after; null for a rate that applies to every claim) and the version it replaced (before; null when
 * none, or the rate applies to every claim): a claim added before it uses that one instead (see {@link #seenBy}).
 */
public record Rate(String grade, String pct, String from, String to, int level, int version, Long after, Rate before) {

    /** The most levels up a sheet's parents give it rates. */
    public static final int LEVELS = 3;

    /** How many levels further up the rates of the sheet a sheet was cloned from are than its own. */
    public static final int SOURCE_LEVELS = LEVELS + 1;

    /** A sheet's own rate. */
    public Rate(String grade, String pct, String from, String to) {
        this(grade, pct, from, to, 1);
    }

    /** A sheet's own rate at its version. */
    public Rate(String grade, String pct, String from, String to, int version) {
        this(grade, pct, from, to, 0, version, null, null);
    }

    /** This rate as one inherited from the levels up. */
    public Rate up(int levels) {
        return new Rate(grade, pct, from, to, level + levels, version, after, before == null ? null : before.up(levels));
    }

    /**
     * The rates as a claim added at the sequence value sees them (null for a claim kept from before it was recorded,
     * which counts as added before every forward-only rate): each forward-only rate saved after it gives way to the
     * version it replaced, or is left out when it replaced none.
     */
    public static List<Rate> seenBy(List<Rate> rates, Long added) {
        List<Rate> seen = new ArrayList<>(rates.size());
        for (Rate rate : rates) {
            Rate r = rate;
            while (r != null && r.after() != null && (added == null || added <= r.after())) {
                r = r.before();
            }
            if (r != null) {
                seen.add(r);
            }
        }
        return seen;
    }

    /**
     * The percent the grade's rates give on a date: the one whose from-date is the latest on or before it, or null
     * when the date is before every rate the grade has, or after the to-date of that one. When a level gives none, the
     * next level up is used (through the sheet's parents, then the sheet it was cloned from).
     */
    public static String on(List<Rate> rates, String grade, LocalDate date) {
        int top = rates.stream().mapToInt(Rate::level).max().orElse(0);
        for (int level = 0; level <= top; level++) {
            String pct = on(rates, grade, date, level);
            if (pct != null) {
                return pct;
            }
        }
        return null;
    }

    private static String on(List<Rate> rates, String grade, LocalDate date, int level) {
        return rates.stream().filter((r) -> r.grade().equals(grade) && r.level() == level)
                .filter((r) -> r.from() == null || !LocalDate.parse(r.from()).isAfter(date))
                .max(Comparator.comparing((Rate r) -> r.from() == null ? LocalDate.MIN : LocalDate.parse(r.from())))
                .filter((r) -> r.to() == null || !LocalDate.parse(r.to()).isBefore(date))
                .map(Rate::pct).orElse(null);
    }

    /** An amount times a percent, divided by 100, in hundredths rounded half to even; null when there is no rate. */
    public static BigDecimal due(String amount, String pct) {
        return due(amount, pct, RoundingMode.HALF_EVEN);
    }

    /**
     * An amount times a percent, divided by 100, in hundredths rounded by the mode (on the magnitude, so a negative
     * amount rounds as its positive would); null when there is no rate.
     */
    public static BigDecimal due(String amount, String pct, RoundingMode rounding) {
        return pct == null ? null
                : new BigDecimal(amount).multiply(new BigDecimal(pct)).movePointLeft(2).setScale(2, rounding);
    }
}

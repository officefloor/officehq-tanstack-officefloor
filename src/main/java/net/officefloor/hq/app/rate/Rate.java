package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * A grade's rate on a sheet: a percent with two decimals, from a date (null: from the beginning of
 * time) through a date (null: with no end). Its level is how far up the sheet's parent chain it is
 * set: 0 on the sheet itself, 1 on its parent, up to {@link #MAX_LEVEL}.
 */
public record Rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to, int level) {

    /** How many parent sheets up a rate is looked for. */
    public static final int MAX_LEVEL = 3;

    /** A rate set on the sheet itself. */
    public Rate(String sheetId, String grade, BigDecimal pct, LocalDate from, LocalDate to) {
        this(sheetId, grade, pct, from, to, 0);
    }

    /** The same rate seen from a sheet {@code levels} below the one it is set on. */
    public Rate up(int levels) {
        return new Rate(sheetId, grade, pct, from, to, level + levels);
    }

    /**
     * The grade's rate in force on the date: on the sheet itself, else on its parent, and so on up
     * the levels. Empty when no level has one in force.
     */
    public static Optional<BigDecimal> inForce(List<Rate> rates, String grade, LocalDate date) {
        for (int level = 0; level <= MAX_LEVEL; level++) {
            Optional<BigDecimal> found = inForce(rates, grade, date, level);
            if (found.isPresent()) return found;
        }
        return Optional.empty();
    }

    /**
     * The grade's rate in force on the date at one level: the one whose from-date is the latest on
     * or before it, unless its to-date is before the date. Empty when the date is before every rate
     * the grade has there, or after the to-date of the rate that would apply.
     */
    private static Optional<BigDecimal> inForce(List<Rate> rates, String grade, LocalDate date, int level) {
        return rates.stream()
                .filter((r) -> r.level() == level)
                .filter((r) -> r.grade().equals(grade))
                .filter((r) -> r.from() == null || !r.from().isAfter(date))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .filter((r) -> r.to() == null || !r.to().isBefore(date))
                .map(Rate::pct);
    }
}

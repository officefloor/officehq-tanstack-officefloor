package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** A grade's rate on a sheet: a percent with two decimals, from a date (null: from the beginning of time). */
public record Rate(String sheetId, String grade, BigDecimal pct, LocalDate from) {

    /**
     * The grade's rate in force on the date: the one whose from-date is the latest on or before it.
     * Empty when the date is before every rate the grade has.
     */
    public static Optional<BigDecimal> inForce(List<Rate> rates, String grade, LocalDate date) {
        return rates.stream()
                .filter((r) -> r.grade().equals(grade))
                .filter((r) -> r.from() == null || !r.from().isAfter(date))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Rate::pct);
    }
}

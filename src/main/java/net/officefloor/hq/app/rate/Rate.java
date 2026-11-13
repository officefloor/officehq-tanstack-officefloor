package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

/**
 * A rate a sheet has for a grade: a percent with two decimals, in force from {@code from} (null: from
 * the beginning of time) until the grade's next rate takes over.
 */
public record Rate(String sheetId, String grade, BigDecimal rate, LocalDate from) {

    /** The grade's rate in force on the date: the one with the latest from-date on or before it; null with none. */
    public static BigDecimal inForce(List<Rate> rates, String grade, LocalDate date) {
        return rates.stream()
                .filter((r) -> r.grade().equals(grade) && (r.from() == null || !r.from().isAfter(date)))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .map(Rate::rate).orElse(null);
    }
}

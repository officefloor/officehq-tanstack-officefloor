package net.officefloor.hq.app.rate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** A grade's rate on a sheet: a percent with two decimals, in force from its from-date (null: from the
 *  beginning of time). */
public record Rate(String sheetId, String grade, BigDecimal pct, LocalDate from) {

    /** The grade's rate in force on the date: the one with the latest from-date on or before it. */
    public static Optional<Rate> inForce(List<Rate> rates, String grade, LocalDate on) {
        return rates.stream()
                .filter(r -> r.grade().equals(grade))
                .filter(r -> r.from() == null || !r.from().isAfter(on))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())));
    }
}

package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/** The sheet's rates a figure is worked out with: as they are now (RateRepository), or as they stood at the end of a
 *  day (RateRepository.asOf). */
@FunctionalInterface
public interface Rates {

    /** The grade's rate in force on the date; empty when none applies. */
    Optional<Rate> on(String sheetId, String grade, LocalDate date);

    /** Of the rates, the grade's in force on the date: the one whose from-date is the latest on or before it (no
     *  from-date counting as the beginning of time); empty when none applies, or that one's to-date is before the date. */
    static Optional<Rate> on(List<Rate> rates, String grade, LocalDate date) {
        return rates.stream().filter(r -> r.grade().equals(grade) && r.appliesOn(date))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .filter(r -> !r.endedBy(date));
    }
}

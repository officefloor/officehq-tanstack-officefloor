package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** The sheet's rates a figure is worked out with: as they are now (RateRepository), or as they stood at the end of a
 *  day (RateRepository.asOf). */
@FunctionalInterface
public interface Rates {

    /** The grade's rate in force on the date for the claim (see Rate.reaches; 0: without regard to any claim); empty
     *  when none applies. */
    Optional<Rate> on(String sheetId, String grade, LocalDate date, long claimId);

    /** The grade's rate in force on the date, without regard to any claim; empty when none applies. */
    default Optional<Rate> on(String sheetId, String grade, LocalDate date) {
        return on(sheetId, grade, date, 0);
    }

    /** Of every version of a sheet's rates (oldest first), the ones in force for the claim: for each grade and
     *  from-date, the newest version that reaches it (see Rate.reaches; 0: the newest of all), each grade's earliest
     *  first. */
    static List<Rate> forClaim(List<Rate> versions, long claimId) {
        Map<String, Rate> byGradeFrom = new LinkedHashMap<>();
        for (Rate r : versions) {
            if (r.reaches(claimId)) {
                byGradeFrom.put(r.grade() + "|" + r.from(), r);
            }
        }
        return byGradeFrom.values().stream().sorted(Comparator.comparing(Rate::grade)
                .thenComparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder()))).toList();
    }

    /** Of the rates, the grade's in force on the date: the one whose from-date is the latest on or before it (no
     *  from-date counting as the beginning of time); empty when none applies, or that one's to-date is before the date. */
    static Optional<Rate> on(List<Rate> rates, String grade, LocalDate date) {
        return rates.stream().filter(r -> r.grade().equals(grade) && r.appliesOn(date))
                .max(Comparator.comparing(Rate::from, Comparator.nullsFirst(Comparator.naturalOrder())))
                .filter(r -> !r.endedBy(date));
    }
}

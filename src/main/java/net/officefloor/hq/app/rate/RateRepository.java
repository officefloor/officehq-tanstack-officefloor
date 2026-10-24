package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateRepository extends JpaRepository<Rate, Long> {

    /** The sheet's rates, by grade, then by from-date (a rate with none first). */
    List<Rate> findBySheetIdOrderByGradeAscFromDateAscIdAsc(String sheetId);

    List<Rate> findBySheetIdAndGrade(String sheetId, String grade);

    /** The grade's rate with this from-date (null: the one with none). */
    default Optional<Rate> findWithFrom(String sheetId, String grade, LocalDate from) {
        return findBySheetIdAndGrade(sheetId, grade).stream()
                .filter(r -> from == null ? r.getFromDate() == null : from.equals(r.getFromDate()))
                .findFirst();
    }

    /** The grade's rate in force on a date: the one whose from-date is the latest on or before it, a rate with
     *  no from-date applying from the beginning of time; empty when none applies. */
    default Optional<Rate> findInForce(String sheetId, String grade, LocalDate on) {
        return findBySheetIdAndGrade(sheetId, grade).stream()
                .filter(r -> r.getFromDate() == null || !r.getFromDate().isAfter(on))
                .max(Comparator.comparing(Rate::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder())));
    }
}

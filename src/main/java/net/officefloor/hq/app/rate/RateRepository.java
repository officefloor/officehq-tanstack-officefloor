package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateRepository extends JpaRepository<Rate, Long> {

    /** Orders a grade's rates by from-date, none (the beginning of time) first. */
    Comparator<Rate> BY_FROM = Comparator.comparing(Rate::getFromDate, Comparator.nullsFirst(Comparator.naturalOrder()));

    /** The sheet's rates, by grade then from-date (none first). */
    default List<Rate> findBySheetIdOrdered(String sheetId) {
        return findBySheetId(sheetId).stream()
                .sorted(Comparator.comparing(Rate::getGrade).thenComparing(BY_FROM)).toList();
    }

    List<Rate> findBySheetId(String sheetId);

    List<Rate> findBySheetIdAndGrade(String sheetId, String grade);

    /** The grade's rate with exactly this from-date (null: the one from the beginning of time). */
    default Optional<Rate> findFor(String sheetId, String grade, LocalDate from) {
        return findBySheetIdAndGrade(sheetId, grade).stream()
                .filter((r) -> from == null ? r.getFromDate() == null : from.equals(r.getFromDate())).findFirst();
    }

    /** The grade's rate in force on the date: the latest from-date on or before it; none before them all. */
    default Optional<Rate> findInForce(String sheetId, String grade, LocalDate date) {
        return findBySheetIdAndGrade(sheetId, grade).stream()
                .filter((r) -> r.getFromDate() == null || !r.getFromDate().isAfter(date)).max(BY_FROM);
    }
}

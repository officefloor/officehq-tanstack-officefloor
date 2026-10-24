package net.officefloor.hq.app.holiday;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HolidayRepository extends JpaRepository<Holiday, Long> {

    /** The sheet's holidays, by date. */
    List<Holiday> findBySheetIdOrderByDateAsc(String sheetId);

    boolean existsBySheetIdAndDate(String sheetId, LocalDate date);

    /** The dates of the sheet's holidays. */
    default Set<LocalDate> datesOf(String sheetId) {
        return findBySheetIdOrderByDateAsc(sheetId).stream().map(Holiday::getDate).collect(Collectors.toSet());
    }

    /** The terms-th business day after a date, skipping the weekends and the sheet's holidays now on its list. */
    default LocalDate dueBy(String sheetId, LocalDate from, int terms) {
        return BusinessDays.after(from, terms, datesOf(sheetId));
    }
}

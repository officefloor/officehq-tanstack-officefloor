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

    /** The date itself when it is not one of the sheet's holidays, else the next day that is not. */
    default LocalDate nextNonHoliday(String sheetId, LocalDate date) {
        Set<LocalDate> holidays = findBySheetIdOrderByDateAsc(sheetId).stream().map(Holiday::getDate)
                .collect(Collectors.toSet());
        LocalDate day = date;
        while (holidays.contains(day)) {
            day = day.plusDays(1);
        }
        return day;
    }
}

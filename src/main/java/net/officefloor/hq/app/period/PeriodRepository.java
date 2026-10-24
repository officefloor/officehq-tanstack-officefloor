package net.officefloor.hq.app.period;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodRepository extends JpaRepository<Period, Long> {

    /** The sheet's periods, by from-date. */
    List<Period> findBySheetIdOrderByFromDateAscIdAsc(String sheetId);

    Optional<Period> findByIdAndSheetId(Long id, String sheetId);

    /** Whether a date falls in one of the sheet's closed periods. */
    default boolean isClosedOn(String sheetId, LocalDate date) {
        return date != null && findBySheetIdOrderByFromDateAscIdAsc(sheetId).stream()
                .anyMatch(p -> p.isClosed() && p.contains(date));
    }
}

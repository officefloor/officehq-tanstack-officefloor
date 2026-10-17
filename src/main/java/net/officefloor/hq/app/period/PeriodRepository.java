package net.officefloor.hq.app.period;

import java.util.Comparator;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PeriodRepository extends JpaRepository<Period, Long> {

    /** The sheet's periods, by from-date. */
    default List<Period> findBySheetIdOrdered(String sheetId) {
        return findBySheetId(sheetId).stream().sorted(Comparator.comparing(Period::getFromDate)).toList();
    }

    List<Period> findBySheetId(String sheetId);
}

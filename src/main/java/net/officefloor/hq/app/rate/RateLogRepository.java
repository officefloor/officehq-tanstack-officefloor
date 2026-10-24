package net.officefloor.hq.app.rate;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RateLogRepository extends JpaRepository<RateLog, Long> {

    /** The sheet's rates set, in the order set. */
    List<RateLog> findBySheetIdOrderByIdAsc(String sheetId);

    /** The sheet's rates as they stood at the end of a date: every rate set while the sheet's today was on or before
     *  it, each replacing the one its grade had for its from-date, as setting it did. Only worked out, never saved. */
    default List<Rate> ratesAsOf(String sheetId, LocalDate date) {
        List<Rate> rates = new ArrayList<>();
        for (RateLog set : findBySheetIdOrderByIdAsc(sheetId)) {
            if (set.getMadeOn().isAfter(date)) {
                continue;
            }
            Rate rate = rates.stream().filter(r -> r.getGrade().equals(set.getGrade())
                    && Objects.equals(r.getFromDate(), set.getFromDate())).findFirst().orElse(null);
            if (rate == null) {
                rates.add(new Rate(sheetId, set.getGrade(), set.getPct(), set.getFromDate(), set.getToDate()));
            } else {
                rate.setPct(set.getPct());
                rate.setToDate(set.getToDate());
            }
        }
        return rates;
    }
}

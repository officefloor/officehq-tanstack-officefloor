package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: a DRAFT claim of the amount on every plan date up to the end; {@code generated} how many
 *  claims it has made so far. The plan dates are the start then every {@code every} days, or, for a monthly plan
 *  ({@code day} set, {@code every} null), {@link #dates(LocalDate)}. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every,
        Integer day, LocalDate end, int generated) {

    /** The plan dates up to and including {@code last}, in order. A monthly plan's are its day in every month
     *  from the start's month, a day beyond the month's length falling on its last day, those before the start
     *  left out. */
    public List<LocalDate> dates(LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (day == null) {
            for (LocalDate d = start; !d.isAfter(last); d = d.plusDays(every)) {
                dates.add(d);
            }
            return dates;
        }
        for (YearMonth m = YearMonth.from(start); !m.atDay(1).isAfter(last); m = m.plusMonths(1)) {
            LocalDate d = m.atDay(Math.min(day, m.lengthOfMonth()));
            if (!d.isBefore(start) && !d.isAfter(last)) {
                dates.add(d);
            }
        }
        return dates;
    }
}

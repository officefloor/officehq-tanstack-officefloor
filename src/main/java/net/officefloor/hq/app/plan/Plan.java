package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/**
 * A person's plan: an amount, a start date, every N days or a day of the month (exactly one set) and an end date (not
 * before the start), and how many claims it has made so far.
 */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every,
        Integer day, LocalDate end, int generated) {

    /**
     * Its dates up to the end and {@code until}, earliest first: the start, then every N days; or, monthly, the day in
     * every month from the start's month (a day beyond the month's length on its last day), those before the start left
     * out.
     */
    public List<LocalDate> datesThrough(LocalDate until) {
        List<LocalDate> dates = new ArrayList<>();
        LocalDate last = end.isBefore(until) ? end : until;
        if (day != null) {
            for (YearMonth m = YearMonth.from(start); !m.atDay(1).isAfter(last); m = m.plusMonths(1)) {
                LocalDate d = m.atDay(Math.min(day, m.lengthOfMonth()));
                if (!d.isBefore(start) && !d.isAfter(last)) {
                    dates.add(d);
                }
            }
            return dates;
        }
        for (LocalDate d = start; !d.isAfter(last); d = d.plusDays(every)) {
            dates.add(d);
        }
        return dates;
    }

    /** Its first five dates after {@code today} and on or before the end, earliest first. */
    public List<LocalDate> nextDates(LocalDate today) {
        return datesThrough(end).stream().filter(d -> d.isAfter(today)).limit(5).toList();
    }
}

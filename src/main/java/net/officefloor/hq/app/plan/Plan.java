package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: the amount, from the start every N days (or monthly on a day of the month) up to the end; generated is
 *  how many claims it has made. Exactly one of every and day is set. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end,
        int generated) {

    /** The plan dates up to and including last: the start then every N days, or the day in every month from the start's month
     *  (on the month's last day when the month is shorter), dates before the start left out. */
    public List<LocalDate> dates(LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (day != null) {
            for (YearMonth month = YearMonth.from(start); ; month = month.plusMonths(1)) {
                LocalDate date = month.atDay(Math.min(day, month.lengthOfMonth()));
                if (date.isAfter(last)) break;
                if (!date.isBefore(start)) dates.add(date);
            }
        } else {
            for (LocalDate date = start; !date.isAfter(last); date = date.plusDays(every)) {
                dates.add(date);
            }
        }
        return dates;
    }

    /** The first count plan dates after today and on or before the end. */
    public List<LocalDate> nextDates(LocalDate today, int count) {
        return dates(end).stream().filter(d -> d.isAfter(today)).limit(count).toList();
    }
}

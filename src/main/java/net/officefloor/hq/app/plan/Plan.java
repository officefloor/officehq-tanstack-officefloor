package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: its amount (two decimals), start, every N days or else a day of the month (1 to 31),
 *  end, who added it, and how many claims it has made so far. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every,
        Integer day, LocalDate end, String by, int generated) {

    /** The plan dates up to (and including) the last date: the start then every N days, or the day of the
     *  month in every month from the start's month (a day beyond the month's length on its last day),
     *  leaving out dates before the start. */
    public List<LocalDate> dates(LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (day != null) {
            for (LocalDate month = start.withDayOfMonth(1); !month.isAfter(last); month = month.plusMonths(1)) {
                LocalDate date = month.withDayOfMonth(Math.min(day, month.lengthOfMonth()));
                if (!date.isBefore(start) && !date.isAfter(last)) dates.add(date);
            }
        } else {
            for (LocalDate date = start; !date.isAfter(last); date = date.plusDays(every)) dates.add(date);
        }
        return dates;
    }
}

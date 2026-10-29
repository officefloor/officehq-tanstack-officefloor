package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.ArrayList;
import java.util.List;

/** A person's plan: a DRAFT claim of {@code amount} on the start date, then every {@code every} days, up to the end date;
 *  or, when {@code day} is 1 to 31 (and {@code every} 0), on that day of every month from the start's month (a day beyond
 *  the month's length on its last day), dates before the start left out. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every, LocalDate end,
        int day) {

    public boolean monthly() {
        return day > 0;
    }

    /** The plan's dates up to and including {@code last}, in order. */
    public List<LocalDate> dates(LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (monthly()) {
            for (LocalDate month = start.withDayOfMonth(1); !month.isAfter(last); month = month.plusMonths(1)) {
                LocalDate date = month.withDayOfMonth(Math.min(day, month.lengthOfMonth()));
                if (!date.isBefore(start) && !date.isAfter(last)) {
                    dates.add(date);
                }
            }
        } else {
            for (LocalDate date = start; !date.isAfter(last); date = date.plusDays(every)) {
                dates.add(date);
            }
        }
        return dates;
    }
}

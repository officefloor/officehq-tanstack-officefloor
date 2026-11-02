package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: an amount (two decimals), from {@code start} every {@code every} days (or, when {@code day} is set,
 *  monthly on that day of the month) to {@code end} (ISO dates),
 *  with how many claims it has {@code generated} so far, and its {@code nextDates}: the first five plan dates after
 *  the sheet's today and on or before the end. */
public record Plan(long id, long person, String amount, String start, int every, String end, int generated, Integer day,
        List<String> nextDates) {

    /** How many dates {@code nextDates} shows at most. */
    static final int NEXT = 5;

    /** The plan's dates after {@code after} (none left out when null) and up to {@code last}, at most {@code limit}:
     *  the start then every N days, or, for a monthly plan, its day in every month from the start's month (on the
     *  month's last day when shorter), leaving out any before the start. */
    static List<LocalDate> dates(LocalDate start, int every, Integer day, LocalDate after, LocalDate last, int limit) {
        List<LocalDate> dates = new ArrayList<>();
        if (day == null) {
            for (LocalDate d = start; !d.isAfter(last) && dates.size() < limit; d = d.plusDays(every)) {
                if (after == null || d.isAfter(after)) {
                    dates.add(d);
                }
            }
            return dates;
        }
        for (YearMonth month = YearMonth.from(start); !month.atDay(1).isAfter(last) && dates.size() < limit;
                month = month.plusMonths(1)) {
            LocalDate d = month.atDay(Math.min(day, month.lengthOfMonth()));
            if (!d.isBefore(start) && !d.isAfter(last) && (after == null || d.isAfter(after))) {
                dates.add(d);
            }
        }
        return dates;
    }
}

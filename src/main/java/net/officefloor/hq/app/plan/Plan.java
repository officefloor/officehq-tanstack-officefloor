package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: an amount (two decimals), from {@code start} every {@code every} days (or, when {@code day} is set,
 *  monthly on that day of the month) to {@code end} (ISO dates),
 *  with how many claims it has {@code generated} so far, and its {@code nextDates}: the first five plan dates after
 *  the sheet's today and on or before the end, leaving out the dates it was paused for; and whether it is
 *  {@code paused} now (paused and not yet resumed); and its amount {@code changes}, oldest first, each as
 *  {@code <date> amount=<amount>}; and the descendant {@code sheet} it generates its claims on (null: its own). {@code amount} is the current one: the amount of the claims it generates next. */
public record Plan(long id, long person, String amount, String start, int every, String end, int generated, Integer day,
        List<String> nextDates, boolean paused, List<String> changes, String sheet) {

    /** A pause: from the day paused up to, not including, the day resumed (null: still paused). */
    record Pause(LocalDate from, LocalDate until) {
        boolean covers(LocalDate d) {
            return !d.isBefore(from) && (until == null || d.isBefore(until));
        }
    }

    /** How many dates {@code nextDates} shows at most. */
    static final int NEXT = 5;

    /** The plan's dates after {@code after} (none left out when null) and up to {@code last}, at most {@code limit}:
     *  the start then every N days, or, for a monthly plan, its day in every month from the start's month (on the
     *  month's last day when shorter), leaving out any before the start and any a pause covers. */
    static List<LocalDate> dates(LocalDate start, int every, Integer day, List<Pause> pauses, LocalDate after,
            LocalDate last, int limit) {
        List<LocalDate> dates = new ArrayList<>();
        if (day == null) {
            for (LocalDate d = start; !d.isAfter(last) && dates.size() < limit; d = d.plusDays(every)) {
                if ((after == null || d.isAfter(after)) && !paused(pauses, d)) {
                    dates.add(d);
                }
            }
            return dates;
        }
        for (YearMonth month = YearMonth.from(start); !month.atDay(1).isAfter(last) && dates.size() < limit;
                month = month.plusMonths(1)) {
            LocalDate d = month.atDay(Math.min(day, month.lengthOfMonth()));
            if (!d.isBefore(start) && !d.isAfter(last) && (after == null || d.isAfter(after))
                    && !paused(pauses, d)) {
                dates.add(d);
            }
        }
        return dates;
    }

    private static boolean paused(List<Pause> pauses, LocalDate d) {
        return pauses.stream().anyMatch(p -> p.covers(d));
    }
}

package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: a claim of `amount` on the start date and every `every` days after, or (`every`
 *  null) monthly on `day` of the month, up to the end date; `generated` is how many claims it has
 *  made so far; `pauses` its pauses in the order made, a plan date in one skipped; `changes` its
 *  changes of amount, oldest first (`amount` is the latest). */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every,
        Integer day, LocalDate end, int generated, List<PlanPause> pauses, List<PlanChange> changes) {

    /** Whether the plan is paused now: it has a pause not yet resumed. */
    public boolean paused() {
        return pauses.stream().anyMatch(p -> p.resumedOn() == null);
    }

    private boolean skipped(LocalDate date) {
        return pauses.stream().anyMatch(p -> p.skips(date));
    }

    /** The plan dates from the start up to `last`, in order: the start then every N days, or the
     *  day of the month in every month from the start's month (a day beyond the month's length on
     *  its last day), dates before the start and dates in a pause left out. */
    public List<LocalDate> dates(LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (every != null) {
            for (LocalDate date = start; !date.isAfter(last); date = date.plusDays(every)) dates.add(date);
        } else {
            for (YearMonth month = YearMonth.from(start); !month.atDay(1).isAfter(last); month = month.plusMonths(1)) {
                LocalDate date = month.atDay(Math.min(day, month.lengthOfMonth()));
                if (!date.isBefore(start) && !date.isAfter(last)) dates.add(date);
            }
        }
        dates.removeIf(this::skipped);
        return dates;
    }

    /** The first `count` plan dates after `today` and on or before the end, in order. */
    public List<LocalDate> nextDates(LocalDate today, int count) {
        List<LocalDate> next = new ArrayList<>();
        for (LocalDate date : dates(end)) {
            if (next.size() == count) break;
            if (date.isAfter(today)) next.add(date);
        }
        return next;
    }
}

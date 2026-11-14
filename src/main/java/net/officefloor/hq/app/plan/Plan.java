package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: an amount every {@code every} days, or monthly on {@code day} of the month (exactly one of them is
 * set), from {@code start} to {@code end}; {@code generated} is how many claims it has made so far; {@code pauses} its pauses and {@code changes} its amount changes, oldest first;
 * {@code amount} is its current amount. The id is null until
 * it is saved. */
public record Plan(Long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every,
        Integer day, LocalDate end, int generated, List<PlanPause> pauses,
        List<PlanChange> changes) {

    /** Whether the plan is paused now: it has a pause not yet resumed. */
    public boolean paused() {
        return pauses.stream().anyMatch(p -> p.resumedOn() == null);
    }

    /** The plan's dates up to {@code last} that no pause skips, in order. */
    public List<LocalDate> activeDates(LocalDate last) {
        return dates(last).stream().filter(d -> pauses.stream().noneMatch(p -> p.skips(d))).toList();
    }

    /** The plan's dates from its start up to {@code last}, in order. */
    public List<LocalDate> dates(LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (day != null) {
            for (YearMonth month = YearMonth.from(start); !month.atDay(1).isAfter(last); month = month.plusMonths(1)) {
                LocalDate date = month.atDay(Math.min(day, month.lengthOfMonth()));
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

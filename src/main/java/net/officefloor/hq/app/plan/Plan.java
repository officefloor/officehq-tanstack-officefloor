package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * A person's plan on a sheet: an amount with two decimals, made a DRAFT claim on its start date, then
 * every {@code every} days or monthly on day {@code day} of the month (the other null), up to its end date. {@code generated} is how many claims it has made so far.
 * {@code pauses} are its pauses, in order: a plan date in one is skipped, not postponed. {@code changes} are
 * its amount's changes, oldest first; {@code amount} is the amount now. {@code target} is the descendant sheet
 * its claims are made on, for the person of the same name there (null: its own sheet, for its own person).
 */
public record Plan(Long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every,
        Integer day, LocalDate end, int generated, List<PlanPause> pauses,
        List<PlanChange> changes, String target) {

    /** Whether the plan is paused now: its last pause not yet resumed. */
    public boolean paused() {
        return !pauses.isEmpty() && pauses.get(pauses.size() - 1).resumedOn() == null;
    }

    private boolean skipped(LocalDate date) {
        return pauses.stream().anyMatch(p -> p.skips(date));
    }

    /**
     * The plan dates up to {@code last}, in order: its start then every N days; or, monthly, that day in
     * every month from the start's month (a day beyond the month's length falling on its last day),
     * dates before the start left out, and dates in a pause skipped.
     */
    public List<LocalDate> datesUpTo(LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (day != null) {
            for (LocalDate month = start.withDayOfMonth(1); !month.isAfter(last); month = month.plusMonths(1)) {
                LocalDate date = month.withDayOfMonth(Math.min(day, month.lengthOfMonth()));
                if (!date.isBefore(start) && !date.isAfter(last) && !skipped(date)) dates.add(date);
            }
        } else {
            for (LocalDate date = start; !date.isAfter(last); date = date.plusDays(every)) {
                if (!skipped(date)) dates.add(date);
            }
        }
        return dates;
    }

    /** The first {@code count} plan dates after {@code today} and on or before its end, in order. */
    public List<LocalDate> nextDates(LocalDate today, int count) {
        return datesUpTo(end).stream().filter(d -> d.isAfter(today)).limit(count).toList();
    }
}

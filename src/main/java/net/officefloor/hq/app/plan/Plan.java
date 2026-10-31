package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;

/** A person's plan: the amount, from the start every N days (or monthly on a day of the month) up to the end; generated is
 *  how many claims it has made; pauses its pauses in the order they were taken; changes its amount changes, oldest first. Exactly one of every and day is set. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end,
        int generated, List<Pause> pauses, List<Change> changes) {

    /** A pause: from the day it was paused up to (not including) the day it was resumed, null while still paused. */
    public record Pause(LocalDate from, LocalDate to) {
        boolean skips(LocalDate date) {
            return !date.isBefore(from) && (to == null || date.isBefore(to));
        }
    }

    /** An amount change: on the day the plan's amount became amount. */
    public record Change(LocalDate on, BigDecimal amount) {
    }

    /** Whether the plan is paused now: its latest pause not resumed. */
    public boolean paused() {
        return !pauses.isEmpty() && pauses.get(pauses.size() - 1).to() == null;
    }

    public Plan withPauses(List<Pause> pauses) {
        return new Plan(id, sheetId, personId, amount, start, every, day, end, generated, pauses, changes);
    }

    public Plan withChanges(List<Change> changes) {
        return new Plan(id, sheetId, personId, amount, start, every, day, end, generated, pauses, changes);
    }

    /** Whether a pause skips the date. */
    public boolean skipped(LocalDate date) {
        return pauses.stream().anyMatch(p -> p.skips(date));
    }

    /** The plan dates up to and including last that no pause skips: the start then every N days, or the day in every month from the start's month
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
        dates.removeIf(this::skipped);
        return dates;
    }

    /** The first count plan dates after today and on or before the end; none while the plan is paused. */
    public List<LocalDate> nextDates(LocalDate today, int count) {
        if (paused()) return List.of();
        return dates(end).stream().filter(d -> d.isAfter(today)).limit(count).toList();
    }
}

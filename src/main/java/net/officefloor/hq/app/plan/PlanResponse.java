package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/** A plan as the screen shows it: the amount with two decimals, the dates yyyy-mm-dd and how many claims it has made so far, and
 *  its next dates: the first five after the sheet's today and on or before its end, comma and space separated, left out where a pause skips them; and whether it is paused now (yes/no). */
public record PlanResponse(long id, long personId, String amount, String start, int every, String end, int generated, int day,
        String nextDates, String paused) {

    public static PlanResponse from(Plan plan, int generated, LocalDate today, List<PlanPause> pauses) {
        return new PlanResponse(plan.id(), plan.personId(), plan.amount().toPlainString(), plan.start().toString(),
                plan.every(), plan.end().toString(), generated, plan.day(),
                plan.nextDates(today, 5, pauses).stream().map(LocalDate::toString).collect(Collectors.joining(", ")),
                pauses.stream().anyMatch(p -> p.resumedOn() == null) ? "yes" : "no");
    }
}

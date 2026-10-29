package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.stream.Collectors;

/** A plan as the screen shows it: the amount with two decimals, the dates yyyy-mm-dd and how many claims it has made so far, and
 *  its next dates: the first five after the sheet's today and on or before its end, comma and space separated. */
public record PlanResponse(long id, long personId, String amount, String start, int every, String end, int generated, int day,
        String nextDates) {

    public static PlanResponse from(Plan plan, int generated, LocalDate today) {
        return new PlanResponse(plan.id(), plan.personId(), plan.amount().toPlainString(), plan.start().toString(),
                plan.every(), plan.end().toString(), generated, plan.day(),
                plan.nextDates(today, 5).stream().map(LocalDate::toString).collect(Collectors.joining(", ")));
    }
}

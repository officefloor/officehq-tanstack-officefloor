package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;

/** A plan as the screens show it: amounts with two decimals, dates as yyyy-mm-dd; `nextDates` its
 *  first five plan dates after the sheet's today and on or before its end, the paused ones left out;
 *  `paused` whether it is paused now. */
public record PlanResponse(long id, long personId, String amount, String start, Integer every, Integer day, String end,
        int generated, List<String> nextDates, boolean paused) {
    static PlanResponse from(Plan plan, LocalDate today) {
        return new PlanResponse(plan.id(), plan.personId(), plan.amount().toPlainString(), plan.start().toString(),
                plan.every(), plan.day(), plan.end().toString(), plan.generated(),
                plan.nextDates(today, 5).stream().map(LocalDate::toString).toList(), plan.paused());
    }
}

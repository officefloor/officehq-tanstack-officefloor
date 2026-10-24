package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;

/** A plan as the screen shows it: its person, amount (two decimals), start, every N days or day of the month (the other
 *  null), end, how many claims it has made so far, and its next dates: the first five plan dates after the sheet's
 *  today and on or before its end. */
public record PlanResponse(Long id, Long personId, String amount, String start, Integer every, Integer day, String end,
        long generated, List<String> nextDates) {

    public static PlanResponse of(Plan plan, LocalDate today, PlanClaimRepository made) {
        return new PlanResponse(plan.getId(), plan.getPersonId(), plan.getAmount().setScale(2).toPlainString(),
                plan.getStart().toString(), plan.getEvery(), plan.getDay(),
                plan.getEnd().toString(), made.countByPlanId(plan.getId()),
                plan.nextDatesAfter(today, 5).stream().map(LocalDate::toString).toList());
    }
}

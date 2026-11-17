package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.stream.Collectors;

/** A plan as listed: the plan and its next dates — its first five dates after the sheet's today and on or before its
 *  end (see PlanDates), comma and space separated — and whether it is paused. */
public record PlanView(long id, long personId, BigDecimal amount, LocalDate start, Integer every, Integer day,
        LocalDate end, int generated, String nextDates, boolean paused) {

    public static PlanView of(Plan plan, LocalDate today) {
        String next = PlanDates.after(plan, today, 5).stream().map(LocalDate::toString)
                .collect(Collectors.joining(", "));
        return new PlanView(plan.id(), plan.personId(), plan.amount(), plan.start(), plan.every(), plan.day(),
                plan.end(), plan.generated(), next, plan.paused());
    }
}

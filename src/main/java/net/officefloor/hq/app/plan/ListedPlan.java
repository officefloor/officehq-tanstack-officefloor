package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A plan as listed: the plan with its next dates — the first five plan dates after the sheet's today
 *  and on or before its end, leaving out the days it was paused on — and whether it is paused. */
public record ListedPlan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, Integer every,
        Integer day, LocalDate end, String by, int generated, List<LocalDate> nextDates,
        boolean paused) {

    static ListedPlan of(Plan plan, LocalDate today) {
        List<LocalDate> next = plan.dates(plan.end()).stream().filter((d) -> d.isAfter(today)).limit(5).toList();
        return new ListedPlan(plan.id(), plan.sheetId(), plan.personId(), plan.amount(), plan.start(), plan.every(),
                plan.day(), plan.end(), plan.by(), plan.generated(), next, plan.paused());
    }
}

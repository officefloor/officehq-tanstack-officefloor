package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** A plan's dates up to a last date: the start, then every N days; or, monthly, the plan's day in every month from the
 *  start's month (a day beyond the month's length falling on its last day), dates before the start left out. */
public final class PlanDates {
    private PlanDates() {
    }

    public static List<LocalDate> upTo(Plan plan, LocalDate last) {
        List<LocalDate> dates = new ArrayList<>();
        if (plan.day() != null) {
            for (LocalDate month = plan.start().withDayOfMonth(1); !month.isAfter(last); month = month.plusMonths(1)) {
                LocalDate date = month.withDayOfMonth(Math.min(plan.day(), month.lengthOfMonth()));
                if (!date.isBefore(plan.start()) && !date.isAfter(last)) {
                    dates.add(date);
                }
            }
        } else {
            for (LocalDate date = plan.start(); !date.isAfter(last); date = date.plusDays(plan.every())) {
                dates.add(date);
            }
        }
        return dates;
    }
}

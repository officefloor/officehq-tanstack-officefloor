package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;

/** A plan as the screen shows it (amount with two decimals, dates as yyyy-mm-dd; every or day, the other null); nextDates are its
 *  first five plan dates after the sheet's today and on or before the end (none while paused); paused whether it is paused now. */
public record PlanResponse(String id, String personId, String amount, String start, Integer every, Integer day, String end, int generated,
        List<String> nextDates, boolean paused) {
    public static PlanResponse from(Plan p, LocalDate today) {
        return new PlanResponse(String.valueOf(p.id()), String.valueOf(p.personId()), p.amount().toPlainString(), p.start().toString(),
                p.every(), p.day(), p.end().toString(), p.generated(), p.nextDates(today, 5).stream().map(LocalDate::toString).toList(), p.paused());
    }
}

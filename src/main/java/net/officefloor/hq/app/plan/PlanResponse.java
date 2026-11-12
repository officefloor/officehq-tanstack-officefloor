package net.officefloor.hq.app.plan;

import java.time.LocalDate;

/** A plan as the front end reads it: the amount with two decimals, dates yyyy-MM-dd; {@code nextDates} its first
 *  five plan dates after the sheet's today and on or before the end, comma and space separated. */
public record PlanResponse(long id, long personId, String amount, String start, Integer every, Integer day,
        String end, int generated, String nextDates) {

    public static PlanResponse from(Plan p, LocalDate today) {
        return new PlanResponse(p.id(), p.personId(), p.amount().toPlainString(), p.start().toString(), p.every(), p.day(),
                p.end().toString(), p.generated(),
                String.join(", ", p.nextDates(today, 5).stream().map(LocalDate::toString).toList()));
    }
}

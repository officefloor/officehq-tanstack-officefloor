package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.stream.Collectors;

/** A plan as the screen reads it: the amount with two decimals, dates as yyyy-mm-dd; {@code nextDates} is its first
 * five dates after the sheet's {@code today} and on or before its end, comma and space separated. */
public record PlanResponse(long id, long personId, String amount, String start, Integer every, Integer day, String end,
        int generated, String nextDates) {
    public static PlanResponse from(Plan p, LocalDate today) {
        String next = p.dates(p.end()).stream().filter(d -> d.isAfter(today)).limit(5).map(LocalDate::toString)
                .collect(Collectors.joining(", "));
        return new PlanResponse(p.id(), p.personId(), p.amount().setScale(2).toPlainString(), p.start().toString(),
                p.every(), p.day(), p.end().toString(), p.generated(), next);
    }
}

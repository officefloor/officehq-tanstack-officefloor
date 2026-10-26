package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;

/** A plan: its person, amount (two decimals), start, every N days or a day of the month (the other null), end, how
 *  many claims it has made so far and its next dates after the sheet's today (see Plan#nextDates). */
public record PlanResponse(long id, long personId, String amount, String start, Integer every, Integer day, String end,
        int generated, List<String> nextDates) {

    static PlanResponse of(Plan p, LocalDate today) {
        return new PlanResponse(p.id(), p.personId(), p.amount().toPlainString(), p.start().toString(), p.every(), p.day(),
                p.end().toString(), p.generated(), p.nextDates(today).stream().map(LocalDate::toString).toList());
    }
}

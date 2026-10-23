package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;

/** A plan as the screen gets it: whose it is, its amount (two decimals), dates (yyyy-mm-dd), every N days or the day of
 *  the month it runs on monthly (the other null), how many claims it has made so far, and its next five plan dates after
 *  the sheet's today (none when the sheet has no today), whether it is paused now, and its amount changes, oldest first,
 *  each as "yyyy-mm-dd amount=0.00". */
public record PlanResponse(long id, long personId, String amount, String start, Integer every, Integer day, String end,
        int generated, List<String> nextDates, boolean paused, List<String> changes) {
    public static PlanResponse from(Plan p, LocalDate today) {
        return new PlanResponse(p.id(), p.personId(), p.amount().setScale(2).toPlainString(), p.start().toString(),
                p.every(), p.day(), p.end().toString(), p.generated(),
                today == null ? List.of() : p.nextDates(today, 5).stream().map(LocalDate::toString).toList(),
                p.paused(), p.changes().stream()
                        .map(c -> c.changedOn() + " amount=" + c.amount().setScale(2).toPlainString()).toList());
    }
}

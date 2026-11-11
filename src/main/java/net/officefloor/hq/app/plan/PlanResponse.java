package net.officefloor.hq.app.plan;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

/**
 * A plan, as the screen reads it: its person's id, amount with two decimals, how many claims it has made and
 * its next dates (the first five after the sheet's today, up to its end, comma and space separated, dates in a pause left out) and whether it is paused;
 * {@code changes} its amount's changes, oldest first, each as {@code <date> amount=<amount>};
 * {@code sheet} the descendant sheet its claims are made on (blank: its own).
 */
public record PlanResponse(String id, String personId, String amount, String start, Integer every, Integer day, String end,
        int generated, String nextDates, boolean paused, List<String> changes, String sheet) {
    public static PlanResponse from(Plan p, LocalDate today) {
        return new PlanResponse(String.valueOf(p.id()), String.valueOf(p.personId()), p.amount().toPlainString(),
                p.start().toString(), p.every(), p.day(), p.end().toString(), p.generated(),
                p.nextDates(today, 5).stream().map(LocalDate::toString).collect(Collectors.joining(", ")), p.paused(),
                p.changes().stream().map(c -> c.changedOn() + " amount=" + c.amount().toPlainString()).toList(), p.target() == null ? "" : p.target());
    }
}

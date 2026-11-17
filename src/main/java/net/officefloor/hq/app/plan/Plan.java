package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** A person's plan: a claim of the amount every {@code every} days from the start up to the end — or, with a
 *  {@code day} (1 to 31) instead, on that day of every month (see PlanDates) — and how many claims it has made so far.
 *  Exactly one of {@code every} and {@code day} is set. Its pauses, in the order made, skip the dates they cover; it is
 *  paused while its last pause is not resumed. */
public record Plan(long id, long personId, BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end,
        int generated, List<PlanPause> pauses) {

    public boolean paused() {
        return !pauses.isEmpty() && pauses.get(pauses.size() - 1).until() == null;
    }

    public boolean skips(LocalDate date) {
        return pauses.stream().anyMatch((p) -> p.skips(date));
    }
}

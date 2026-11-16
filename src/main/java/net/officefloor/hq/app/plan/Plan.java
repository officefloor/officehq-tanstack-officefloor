package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's plan: a claim of the amount every {@code every} days from the start up to the end — or, with a
 *  {@code day} (1 to 31) instead, on that day of every month (see PlanDates) — and how many claims it has made so far.
 *  Exactly one of {@code every} and {@code day} is set. */
public record Plan(long id, long personId, BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end,
        int generated) {
}

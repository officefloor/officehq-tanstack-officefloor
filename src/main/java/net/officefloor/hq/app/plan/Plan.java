package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's plan: a claim of the amount every {@code every} days from the start up to the end, and how many claims
 *  it has made so far. */
public record Plan(long id, long personId, BigDecimal amount, LocalDate start, int every, LocalDate end, int generated) {
}

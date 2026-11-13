package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's plan: an amount every {@code every} days from {@code start} to {@code end}; {@code generated} is how many
 * claims it has made so far. The id is null until it is saved. */
public record Plan(Long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every, LocalDate end,
        int generated) {
}

package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person's plan: a DRAFT claim of {@code amount} on the start date, then every {@code every} days, up to the end date. */
public record Plan(long id, String sheetId, long personId, BigDecimal amount, LocalDate start, int every, LocalDate end) {
}

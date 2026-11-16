package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A plan's values once validated, and who adds it. */
public record PlanValues(BigDecimal amount, LocalDate start, int every, LocalDate end, String by) {
}

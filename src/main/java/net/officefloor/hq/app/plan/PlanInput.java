package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan's fields; a null start means the sheet's today. */
public record PlanInput(BigDecimal amount, LocalDate start, int every, LocalDate end) {
}

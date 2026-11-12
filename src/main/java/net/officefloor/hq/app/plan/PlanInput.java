package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan add. */
public record PlanInput(BigDecimal amount, LocalDate start, int every, LocalDate end, String user) {
}

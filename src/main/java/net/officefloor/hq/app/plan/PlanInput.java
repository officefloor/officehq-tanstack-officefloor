package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan add. */
public record PlanInput(String user, BigDecimal amount, LocalDate start, int every, LocalDate end) {
}

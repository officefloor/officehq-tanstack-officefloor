package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan body. */
public record PlanRequestChecked(BigDecimal amount, LocalDate start, int every, LocalDate end, int day) {
}

package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A change of a plan's amount: on the day, the plan's amount became this one. */
public record PlanChange(LocalDate changedOn, BigDecimal amount) {
}

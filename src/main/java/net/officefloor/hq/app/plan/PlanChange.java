package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A change of a plan's amount: on the sheet's today, to the new amount (two decimals). */
public record PlanChange(LocalDate changedOn, BigDecimal amount) {
}

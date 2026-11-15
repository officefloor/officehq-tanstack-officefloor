package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A change of a plan's amount: the sheet's today when it was made and the new amount (two decimals). */
public record PlanChange(LocalDate changedOn, BigDecimal amount) {
}

package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A change of a plan's amount: on the sheet's today it was made, to the new amount (two decimals). */
public record PlanChange(LocalDate on, BigDecimal amount) {
}

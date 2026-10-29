package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A change of a plan's amount: the day it was changed and the new amount. */
public record PlanChange(LocalDate changedOn, BigDecimal amount) {
}

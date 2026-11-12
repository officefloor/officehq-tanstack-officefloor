package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A change of a plan's amount: on {@code date} (the sheet's today) its amount became {@code amount}. */
public record PlanChange(LocalDate date, BigDecimal amount) {
}

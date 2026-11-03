package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A bound plan form: every N days or (monthly) a day of the month; exactly one is set. */
public record PlanInput(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end) {
}

package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan request: either every N days or monthly on a day of the month (the other null). */
public record PlanInput(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end) {
}

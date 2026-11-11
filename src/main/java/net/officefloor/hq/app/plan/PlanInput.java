package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan request: either every N days or monthly on a day of the month (the other null);
 * {@code sheet} the descendant sheet its claims are made on (null: its own). */
public record PlanInput(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end, String sheet) {
}

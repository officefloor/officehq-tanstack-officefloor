package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A plan's values once validated, and who adds it: exactly one of every N days and a day of the month is set; the descendant sheet
 *  its claims are made on, null for its own. */
public record PlanValues(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end, String by, String sheet) {
}

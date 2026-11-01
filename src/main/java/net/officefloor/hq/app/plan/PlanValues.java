package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan body. `start` is null when none was given: the sheet's today is used. Exactly one
 *  of `every` (days) and `day` (of the month) is set. `sheet`
 *  is the descendant sheet its claims are made on, or null for its own. */
public record PlanValues(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end, String sheet, String user) {
}

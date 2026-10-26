package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan's fields; a null start means the sheet's today. Exactly one of every N days and a day of the
 *  month is set. */
public record PlanInput(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end) {
}

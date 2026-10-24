package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan form: the amount (two decimals), the start, either every N days (at least 1) or a day of the month
 *  (1 to 31), the other null, and the end (not before the start). */
public record PlanInput(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end) {
}

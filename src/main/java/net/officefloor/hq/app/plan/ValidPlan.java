package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A plan request once checked: the amount to two decimals, the start, every N days (N >= 1) or else a day
 *  of the month (1 to 31), and the end (not before the start). */
public record ValidPlan(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end) {
}

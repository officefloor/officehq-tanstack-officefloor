package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan add: either every N days, or monthly on a day of the month (the other is null). */
public record PlanInput(String user, BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end) {
}

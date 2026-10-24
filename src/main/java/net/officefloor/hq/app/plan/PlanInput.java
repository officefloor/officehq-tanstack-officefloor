package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan form: the amount (two decimals), the start, every N days (at least 1) and the end (not before the start). */
public record PlanInput(BigDecimal amount, LocalDate start, int every, LocalDate end) {
}

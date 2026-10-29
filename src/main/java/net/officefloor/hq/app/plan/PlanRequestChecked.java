package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan body; {@code sheet} the descendant it makes its claims on, "" for its own sheet. */
public record PlanRequestChecked(BigDecimal amount, LocalDate start, int every, LocalDate end, int day, String sheet) {
}

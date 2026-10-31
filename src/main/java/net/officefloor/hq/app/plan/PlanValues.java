package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated plan body. `start` is null when none was given: the sheet's today is used. */
public record PlanValues(BigDecimal amount, LocalDate start, int every, LocalDate end, String user) {
}

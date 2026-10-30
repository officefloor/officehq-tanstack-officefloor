package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A plan form that passed validation; a null start takes the sheet's today. */
public record ValidPlan(BigDecimal amount, LocalDate start, int every, LocalDate end, String user) {
}

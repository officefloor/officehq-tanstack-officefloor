package net.officefloor.hq.app.plan;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A plan form that passed validation; a null start takes the sheet's today. Exactly one of every and day is set. */
public record ValidPlan(BigDecimal amount, LocalDate start, Integer every, Integer day, LocalDate end, String user) {
}

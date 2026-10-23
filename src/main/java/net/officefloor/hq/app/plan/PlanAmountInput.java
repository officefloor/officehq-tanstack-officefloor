package net.officefloor.hq.app.plan;

import java.math.BigDecimal;

/** A checked change of a plan's amount: who is doing it, and the new amount (two decimals). */
public record PlanAmountInput(String user, BigDecimal amount) {
}

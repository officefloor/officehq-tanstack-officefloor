package net.officefloor.hq.app.plan;

import java.math.BigDecimal;

/** A validated new plan amount: 0.00 or more, two decimals. */
public record PlanAmountInput(BigDecimal amount) {
}

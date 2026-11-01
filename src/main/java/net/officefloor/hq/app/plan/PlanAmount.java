package net.officefloor.hq.app.plan;

import java.math.BigDecimal;

/** A plan's new amount, validated: more than 0, two decimals. */
public record PlanAmount(BigDecimal amount) {
}

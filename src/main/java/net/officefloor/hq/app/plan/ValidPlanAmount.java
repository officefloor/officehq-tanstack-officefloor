package net.officefloor.hq.app.plan;

import java.math.BigDecimal;

/** A plan's new amount once checked: two decimals, not negative. */
public record ValidPlanAmount(BigDecimal amount) {
}

package net.officefloor.hq.app.plan;

import jakarta.validation.constraints.Size;

/** A plan's new amount as the plan's row submits it. */
public record PlanAmountRequest(@Size(max = 30) String amount) {
}

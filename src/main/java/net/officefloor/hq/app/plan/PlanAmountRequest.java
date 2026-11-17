package net.officefloor.hq.app.plan;

/** Body of changing a plan's amount: the new amount, and who changes it. */
public record PlanAmountRequest(String amount, String by) {
}

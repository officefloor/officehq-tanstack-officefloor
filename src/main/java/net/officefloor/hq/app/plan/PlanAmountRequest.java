package net.officefloor.hq.app.plan;

/** Body of a change of a plan's amount, and who makes it. */
public record PlanAmountRequest(String amount, String user) {
}

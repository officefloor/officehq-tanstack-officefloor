package net.officefloor.hq.app.plan;

/** Body of a change of a plan's amount: the new amount and who saves it. */
public record PlanAmountRequest(String amount, String user) {
}

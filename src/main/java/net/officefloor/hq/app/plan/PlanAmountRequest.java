package net.officefloor.hq.app.plan;

/** Body of changing a plan's amount: who is doing it, and the new amount as typed. */
public record PlanAmountRequest(String user, String amount) {
}

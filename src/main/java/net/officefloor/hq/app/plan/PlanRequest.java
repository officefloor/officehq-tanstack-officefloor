package net.officefloor.hq.app.plan;

/** Body of a plan add, as typed: the amount, the start date, every how many days, the end date and who adds it. */
public record PlanRequest(String amount, String start, String every, String end, String user) {
}

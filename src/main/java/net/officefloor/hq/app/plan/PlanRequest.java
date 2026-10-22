package net.officefloor.hq.app.plan;

/** Body of adding a plan to a person: who is adding it, the amount, start date, every N days and end date as typed. */
public record PlanRequest(String user, String amount, String start, String every, String end) {
}

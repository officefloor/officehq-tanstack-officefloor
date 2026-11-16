package net.officefloor.hq.app.plan;

/** Body of adding a plan: its amount, start date, every N days and end date, and who adds it. */
public record PlanRequest(String amount, String start, String every, String end, String by) {
}

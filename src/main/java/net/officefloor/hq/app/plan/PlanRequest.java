package net.officefloor.hq.app.plan;

/** The plan form as submitted: amount, start date, every N days and end date. */
public record PlanRequest(String amount, String start, String every, String end) {
}

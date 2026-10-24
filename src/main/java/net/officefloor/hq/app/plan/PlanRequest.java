package net.officefloor.hq.app.plan;

/** The plan form as submitted: amount, start date, every N days or a day of the month, and end date. */
public record PlanRequest(String amount, String start, String every, String day, String end) {
}

package net.officefloor.hq.app.plan;

/** The plan form as submitted: amount, start date, every N days or a day of the month, end date, and the descendant sheet
 *  its claims are generated on (blank for its own). */
public record PlanRequest(String amount, String start, String every, String day, String end, String sheet) {
}

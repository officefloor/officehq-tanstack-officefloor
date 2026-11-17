package net.officefloor.hq.app.plan;

/** Body of adding a plan: its amount, start date, every N days or a day of the month, end date, who adds it, and
 *  optionally the descendant sheet its claims are made on (blank for its own). */
public record PlanRequest(String amount, String start, String every, String day, String end, String by, String sheet) {
}

package net.officefloor.hq.app.plan;

/** Body of an add of a plan: an amount, a start date, every N days or else a day of
 *  the month, and an end date. */
public record PlanRequest(String amount, String start, String every, String day, String end) {
}

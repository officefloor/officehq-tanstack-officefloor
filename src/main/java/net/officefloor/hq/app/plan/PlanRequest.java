package net.officefloor.hq.app.plan;

/** Body of an add of a plan: an amount, a start date, every N days or else a day of
 *  the month, an end date, and optionally a descendant sheet to make its claims on. */
public record PlanRequest(String amount, String start, String every, String day, String end, String sheet) {
}

package net.officefloor.hq.app.plan;

/** A plan's fields as typed: amount, start date (blank: the sheet's today), every N days or a day of the month, and end
 *  date, and the signed-in user (`by`) adding it. */
public record PlanRequest(String amount, String start, String every, String day, String end, String by) {
}

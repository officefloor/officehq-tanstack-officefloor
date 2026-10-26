package net.officefloor.hq.app.plan;

/** A plan's fields as typed: amount, start date (blank: the sheet's today), every N days and end date, and the
 *  signed-in user (`by`) adding it. */
public record PlanRequest(String amount, String start, String every, String end, String by) {
}

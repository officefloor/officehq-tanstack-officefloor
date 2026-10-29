package net.officefloor.hq.app.plan;

/** The body of a new plan, as typed: the amount, the start date (yyyy-mm-dd, blank for the sheet's today), every N days or a day of the month (1 to 31) and the end date. */
public record PlanRequest(String amount, String start, String every, String end, String day) {
}

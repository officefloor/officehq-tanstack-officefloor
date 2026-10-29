package net.officefloor.hq.app.plan;

/** The body of a new plan, as typed: the amount, the start date (yyyy-mm-dd, blank for the sheet's today), every N days or a day of the month (1 to 31) and the end date;
 *  and the id of one of the sheet's descendants the claims are made on (blank for the sheet itself). */
public record PlanRequest(String amount, String start, String every, String end, String day, String sheet) {
}

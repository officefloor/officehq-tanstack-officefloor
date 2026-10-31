package net.officefloor.hq.app.plan;

/** Body of a plan added to a person, as typed: the amount, the start date (blank for the sheet's
 *  today), every N days or the day of the month, the end date and who adds it. */
public record PlanRequest(String amount, String start, String every, String day, String end, String user) {
}

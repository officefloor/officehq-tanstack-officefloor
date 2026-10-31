package net.officefloor.hq.app.plan;

/** Body of a plan added to a person, as typed: the amount, the start date (blank for the sheet's
 *  today), every N days, the end date and who adds it. */
public record PlanRequest(String amount, String start, String every, String end, String user) {
}

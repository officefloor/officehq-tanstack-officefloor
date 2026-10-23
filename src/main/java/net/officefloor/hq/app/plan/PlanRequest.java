package net.officefloor.hq.app.plan;

/** Body of adding a plan to a person: who is adding it, the amount, start date, every N days or day of the month (one of the two), and end date as typed. */
public record PlanRequest(String user, String amount, String start, String every, String day, String end) {
}

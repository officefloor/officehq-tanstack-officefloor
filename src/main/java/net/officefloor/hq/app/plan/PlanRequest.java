package net.officefloor.hq.app.plan;

/** Body of a plan add, as typed: the amount, the start date, every how many days or the day of the month, the end
 *  date, who adds it and the descendant sheet its claims are made on (blank for its own). */
public record PlanRequest(String amount, String start, String every, String day, String end, String user,
        String sheet) {
}

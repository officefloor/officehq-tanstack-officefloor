package net.officefloor.hq.app.plan;

/** A plan as the screen sends it: an amount, a start date, every N days or a day of the month, and an end date. */
public record PlanRequest(String amount, String start, String every, String day, String end) {
}

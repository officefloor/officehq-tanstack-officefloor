package net.officefloor.hq.app.plan;

/** Body of POST /api/sheets/{sheet}/people/{person}/plans. */
public record PlanRequest(String amount, String start, String every, String end, String day, String sheet) {
}

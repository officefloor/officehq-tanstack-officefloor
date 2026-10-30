package net.officefloor.hq.app.plan;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/plans: the plan to add. */
public record PlanRequest(String amount, String start, String every, String end, String user) {
}

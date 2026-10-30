package net.officefloor.hq.app.plan;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/plans: the plan to add (every N days, or monthly on a day). */
public record PlanRequest(String amount, String start, String every, String day, String end, String user) {
}

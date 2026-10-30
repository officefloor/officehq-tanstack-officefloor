package net.officefloor.hq.app.plan;

/** Body of POST /api/sheets/{sheetId}/plans/generate: who generates. */
public record GenerateRequest(String user) {
}

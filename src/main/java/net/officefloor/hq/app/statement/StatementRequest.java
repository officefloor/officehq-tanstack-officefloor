package net.officefloor.hq.app.statement;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/statement: who takes it. */
public record StatementRequest(String user) {
}

package net.officefloor.hq.app.statement;

/** Body of POST /api/sheets/{sheetId}/people/{personId}/statement: who takes it, and the date it is taken as of
 *  (yyyy-MM-dd; blank or absent: now). */
public record StatementRequest(String user, String asOf) {
}

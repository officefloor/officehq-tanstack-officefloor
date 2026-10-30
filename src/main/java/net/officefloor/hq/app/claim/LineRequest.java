package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/line: who adds the line, its label, quantity and unit amount. */
public record LineRequest(String label, String qty, String unit, String user) {
}

package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/reassign: the id of the person the claim moves to, and who moves it. */
public record ReassignRequest(String toPersonId, String user) {
}

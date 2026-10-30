package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/waive: who waives the claim's late charge. */
public record WaiveRequest(String user) {
}

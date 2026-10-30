package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/resettle: who re-settles the claim. */
public record ResettleRequest(String user) {
}

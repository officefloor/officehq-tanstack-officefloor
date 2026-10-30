package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/hold: who holds or releases the claim. */
public record HoldClaimRequest(String user) {
}

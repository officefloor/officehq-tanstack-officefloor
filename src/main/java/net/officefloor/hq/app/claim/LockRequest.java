package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/lock and .../unlock: who opens or closes the claim's editor. */
public record LockRequest(String user) {
}

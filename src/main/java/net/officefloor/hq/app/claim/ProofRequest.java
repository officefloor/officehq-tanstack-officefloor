package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/proof: who adds a proof to the claim. */
public record ProofRequest(String user) {
}

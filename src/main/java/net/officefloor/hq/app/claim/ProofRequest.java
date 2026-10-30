package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/proof: who adds a proof to the claim, and its kind
 *  (receipt or photo; blank: none chosen, a receipt). */
public record ProofRequest(String user, String kind) {
}

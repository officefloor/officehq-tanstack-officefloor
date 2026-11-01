package net.officefloor.hq.app.claim;

/** Body of adding a proof to a claim: who adds it, and its kind (receipt or photo; blank or left out for a receipt). */
public record ClaimProofRequest(String user, String kind) {
}

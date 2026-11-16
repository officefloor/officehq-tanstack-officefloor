package net.officefloor.hq.app.claim;

/** Body of adding a proof to a claim: who adds it, and its kind (receipt or photo; blank for none chosen). */
public record ProofRequest(String by, String kind) {
}

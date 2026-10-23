package net.officefloor.hq.app.claim;

/** Body of adding a proof to a claim: who is adding it, and its kind (receipt or photo; null or blank: none chosen,
 *  a receipt). */
public record ProofRequest(String user, String kind) {
}

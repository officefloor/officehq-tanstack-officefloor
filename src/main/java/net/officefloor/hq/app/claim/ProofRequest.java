package net.officefloor.hq.app.claim;

/** The signed-in user (`by`) adding a proof to a claim, of the kind chosen (null or blank: none chosen, a receipt). */
public record ProofRequest(String by, String kind) {
}

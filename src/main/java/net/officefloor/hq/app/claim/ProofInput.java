package net.officefloor.hq.app.claim;

/** A validated proof: who adds it, and the kind chosen (receipt or photo; null when none was chosen, then a receipt). */
public record ProofInput(String user, String kind) {
}

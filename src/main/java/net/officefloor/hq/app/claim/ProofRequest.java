package net.officefloor.hq.app.claim;

/** Body of a claim's proof, as sent: who adds it, and its kind (receipt or photo; blank when none is chosen). */
public record ProofRequest(String user, String kind) {
}

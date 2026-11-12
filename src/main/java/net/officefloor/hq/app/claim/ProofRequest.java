package net.officefloor.hq.app.claim;

/** Body of a claim's proof, as sent: who adds it. */
public record ProofRequest(String user) {
}

package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheet}/claims/{claim}/proof: the proof's kind (receipt or photo), or blank for none chosen. */
public record ProofRequest(String kind) {
}

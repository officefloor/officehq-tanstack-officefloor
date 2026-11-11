package net.officefloor.hq.app.claim;

/** A proof as the screen sends it: its kind, receipt or photo (blank or missing: none chosen, a receipt). */
public record ProofRequest(String kind) {
}

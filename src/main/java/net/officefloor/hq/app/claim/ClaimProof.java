package net.officefloor.hq.app.claim;

/** A proof added once its body is read: who adds it. */
public record ClaimProof(String by) {
}

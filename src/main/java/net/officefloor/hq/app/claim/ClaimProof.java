package net.officefloor.hq.app.claim;

/** A validated proof added to a claim: who adds it, and the kind chosen (null when none was: a receipt). */
public record ClaimProof(String user, String kind) {
}

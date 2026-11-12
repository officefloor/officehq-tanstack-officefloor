package net.officefloor.hq.app.claim;

/** A validated claim status change. */
public record StatusInput(ClaimStatus status, String user) {
}

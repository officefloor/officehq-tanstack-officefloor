package net.officefloor.hq.app.claim;

/** A validated review assignment: who is making it, and the user (trimmed, not blank) to review the claim. */
public record ReviewAssignInput(String user, String reviewer) {
}

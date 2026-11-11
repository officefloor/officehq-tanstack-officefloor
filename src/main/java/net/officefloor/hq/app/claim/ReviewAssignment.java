package net.officefloor.hq.app.claim;

/** A review assignment, read from its request: the user (trimmed) to assign the claim to. */
public record ReviewAssignment(String user) {
}

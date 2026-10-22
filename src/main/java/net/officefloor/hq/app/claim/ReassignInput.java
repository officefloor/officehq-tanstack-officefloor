package net.officefloor.hq.app.claim;

/** A validated reassign: who is making it and the id of the person the claim moves to. */
public record ReassignInput(String user, long personId) {
}

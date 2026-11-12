package net.officefloor.hq.app.claim;

/** A validated move of a claim to the person {@code to} (their id), by {@code user}. */
public record ReassignInput(long to, String user) {
}

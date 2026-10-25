package net.officefloor.hq.app.claim;

/** The signed-in user (`by`) re-settling a SETTLED claim. */
public record ResettleRequest(String by) {
}

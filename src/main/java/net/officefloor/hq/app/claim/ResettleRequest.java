package net.officefloor.hq.app.claim;

/** Body of a settled claim's re-settle, as sent: who re-settles it. */
public record ResettleRequest(String user) {
}

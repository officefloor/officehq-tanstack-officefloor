package net.officefloor.hq.app.claim;

/** Body of a claim's lock or unlock, as sent: who opens or cancels its editor. */
public record LockRequest(String user) {
}

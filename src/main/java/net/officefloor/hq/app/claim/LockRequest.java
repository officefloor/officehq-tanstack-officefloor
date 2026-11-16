package net.officefloor.hq.app.claim;

/** Body of opening or closing a claim's editor: who does it. */
public record LockRequest(String by) {
}

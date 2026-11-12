package net.officefloor.hq.app.claim;

/** Body of a claim's hold toggle, as sent: who holds or releases it. */
public record HoldClaimRequest(String user) {
}

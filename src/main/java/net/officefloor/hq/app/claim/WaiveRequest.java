package net.officefloor.hq.app.claim;

/** Body of a claim's late charge waiver, as sent: who waives it. */
public record WaiveRequest(String user) {
}

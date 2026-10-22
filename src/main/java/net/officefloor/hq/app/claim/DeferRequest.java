package net.officefloor.hq.app.claim;

/** Body of a deferral: who is making it and the date (yyyy-mm-dd) the claim is deferred until. */
public record DeferRequest(String user, String until) {
}

package net.officefloor.hq.app.claim;

/** The date (yyyy-mm-dd) an OPEN claim is deferred until, and the signed-in user (`by`) deferring it. */
public record DeferRequest(String until, String by) {
}

package net.officefloor.hq.app.claim;

/** Body of a deferral: the date (yyyy-mm-dd) the claim is deferred until, and who defers it. */
public record ClaimDeferRequest(String until, String user) {
}

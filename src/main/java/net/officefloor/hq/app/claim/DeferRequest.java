package net.officefloor.hq.app.claim;

/** Body of deferring a claim: the date (yyyy-MM-dd) it is deferred until, and who defers it. */
public record DeferRequest(String until, String by) {
}

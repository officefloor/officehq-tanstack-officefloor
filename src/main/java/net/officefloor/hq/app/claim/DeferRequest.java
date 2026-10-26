package net.officefloor.hq.app.claim;

/** The body of a deferral: {@code until} is the date (yyyy-MM-dd) the claim is deferred to. */
public record DeferRequest(String until) {
}

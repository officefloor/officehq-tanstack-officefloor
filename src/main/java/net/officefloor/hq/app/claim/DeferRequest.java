package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheet}/claims/{claim}/defer: the date (yyyy-MM-dd) the claim is deferred until. */
public record DeferRequest(String until) {
}

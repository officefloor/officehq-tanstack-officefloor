package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/defer: who defers the claim, and the date (yyyy-mm-dd) it is deferred until. */
public record DeferRequest(String user, String until) {
}

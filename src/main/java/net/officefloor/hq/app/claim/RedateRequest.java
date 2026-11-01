package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheet}/claims/{claim}/redate: the claim's new date (yyyy-MM-dd). */
public record RedateRequest(String date) {
}

package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheet}/people/{person}/claims and PUT /api/sheets/{sheet}/claims/{claim}. */
public record ClaimRequest(String amount, String date, String note, String corrects, String ref, String tags) {
}

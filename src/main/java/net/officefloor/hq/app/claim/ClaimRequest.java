package net.officefloor.hq.app.claim;

/** Body (ref: the claim's external reference, blank for none; tags: comma separated, blank for none) of POST /api/sheets/{sheetId}/people/{personId}/claims and PUT /api/sheets/{sheetId}/claims/{claimId}. */
public record ClaimRequest(String amount, String date, String note, String user, String corrects, String ref, String tags) {
}

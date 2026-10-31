package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/share: who shares the claim, with which person, and their share (two decimals). */
public record ShareRequest(String user, String personId, String share) {
}

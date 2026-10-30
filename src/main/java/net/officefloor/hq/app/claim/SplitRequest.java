package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/claims/{claimId}/split: the amount split off the claim, and who splits it. */
public record SplitRequest(String amount, String user) {
}

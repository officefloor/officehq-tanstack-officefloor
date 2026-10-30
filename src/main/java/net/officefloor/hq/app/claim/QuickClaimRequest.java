package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/quick-claims: the person's name and the claim's amount. */
public record QuickClaimRequest(String name, String amount, String user) {
}

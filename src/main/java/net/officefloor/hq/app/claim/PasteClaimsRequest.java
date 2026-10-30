package net.officefloor.hq.app.claim;

/** Body of POST /api/sheets/{sheetId}/paste-claims: the pasted lines and the user adding them. */
public record PasteClaimsRequest(String text, String user) {
}

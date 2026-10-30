package net.officefloor.hq.app.opening;

/** Body of POST /api/sheets/{sheetId}/import-openings: lines of name,period,amount. */
public record ImportOpeningsRequest(String text, String user) {
}

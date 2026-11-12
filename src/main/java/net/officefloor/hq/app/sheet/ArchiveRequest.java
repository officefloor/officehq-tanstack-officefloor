package net.officefloor.hq.app.sheet;

/** Body of POST /api/sheets/{sheetId}/archive: who archives the sheet. */
public record ArchiveRequest(String user) {
}

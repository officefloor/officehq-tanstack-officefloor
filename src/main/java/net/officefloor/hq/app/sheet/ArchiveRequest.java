package net.officefloor.hq.app.sheet;

/** Body of POST /api/sheets/{sheetId}/archive: who archives it. */
public record ArchiveRequest(String user) {
}

package net.officefloor.hq.app.pool;

/** A pool on a sheet: its name (trimmed, at most once on a sheet) and who added it. */
public record Pool(long id, String sheetId, String name, String by) {
}

package net.officefloor.hq.app.pool;

/** A pool on a sheet, added by `by`. */
public record Pool(String sheetId, String name, String by) {
}

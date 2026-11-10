package net.officefloor.hq.app.pool;

/** A pool on a sheet: a trimmed name, at most once per sheet. */
public record Pool(Long id, String sheetId, String name) {
}

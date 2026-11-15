package net.officefloor.hq.app.pool;

/** A pool that went through, and who added it. */
public record PoolAdded(String sheetId, String name, String by) {
}

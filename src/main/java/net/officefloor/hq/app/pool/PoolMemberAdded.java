package net.officefloor.hq.app.pool;

/** A member added to a pool that went through, and who added it. */
public record PoolMemberAdded(String sheetId, String pool, String name, String share, String by) {
}

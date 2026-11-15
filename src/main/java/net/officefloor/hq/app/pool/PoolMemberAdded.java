package net.officefloor.hq.app.pool;

/** A member added to a pool that went through, its dates (null: none), and who added it. */
public record PoolMemberAdded(String sheetId, String pool, String name, String share, String from, String to,
        String by) {
}

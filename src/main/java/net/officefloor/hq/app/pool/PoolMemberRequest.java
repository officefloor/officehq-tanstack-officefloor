package net.officefloor.hq.app.pool;

/** Body of POST /api/sheets/{sheetId}/pool-members: the person to add to the pool, with their share percent. */
public record PoolMemberRequest(String pool, String personId, String share, String user) {
}

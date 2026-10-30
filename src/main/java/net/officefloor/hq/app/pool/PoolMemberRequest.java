package net.officefloor.hq.app.pool;

/** Body of POST /api/sheets/{sheetId}/pool-members: the person (personId) or the other pool (memberPool, its name) to
 *  add to the pool, with their share percent. */
public record PoolMemberRequest(String pool, String personId, String memberPool, String share, String user) {
}

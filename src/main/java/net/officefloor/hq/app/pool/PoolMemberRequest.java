package net.officefloor.hq.app.pool;

/** Body of POST /api/sheets/{sheet}/pool-members: the pool, the person's id and their share percent. */
public record PoolMemberRequest(String pool, Long person, String share) {
}

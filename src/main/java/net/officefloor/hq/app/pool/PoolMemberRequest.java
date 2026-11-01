package net.officefloor.hq.app.pool;

/**
 * Body of POST /api/sheets/{sheet}/pool-members: the pool, the member (a person's id, or another pool's name) and
 * their share percent.
 */
public record PoolMemberRequest(String pool, Long person, String memberPool, String share) {
}

package net.officefloor.hq.app.pool;

/** Body of a member added to a pool: the pool's name, the person, the share percent and who adds it. */
public record PoolMemberRequest(String pool, Long personId, String share, String user) {
}

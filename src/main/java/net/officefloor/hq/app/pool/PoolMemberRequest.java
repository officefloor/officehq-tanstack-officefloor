package net.officefloor.hq.app.pool;

/** The body of adding a member to a pool: the pool's name, the person's id and the share percent as typed. */
public record PoolMemberRequest(String pool, Long personId, String share) {
}

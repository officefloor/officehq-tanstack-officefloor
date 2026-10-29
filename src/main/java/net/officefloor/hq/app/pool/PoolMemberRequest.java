package net.officefloor.hq.app.pool;

/**
 * The body of adding a member to a pool: the pool's name, the person's id or the member pool's name (memberPool), and the
 * share percent as typed.
 */
public record PoolMemberRequest(String pool, Long personId, String share, String memberPool) {
}

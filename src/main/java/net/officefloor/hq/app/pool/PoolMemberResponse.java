package net.officefloor.hq.app.pool;

/** A pool's member as the screen sees it: the person (or, for a member that is a pool, null and the
 *  pool's name in `pool`), their share (two decimals), whether retired, and their cut of the pool's pot
 *  (two decimals; null when the pool is unbalanced or has no pot). */
public record PoolMemberResponse(Long personId, String name, String share, boolean retired, String cut, String pool) {
}

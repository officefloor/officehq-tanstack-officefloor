package net.officefloor.hq.app.pool;

/** A pool's member as the screen sees it: the person, their share (two decimals), whether retired,
 *  and their cut of the sheet's pot (two decimals; null when the pool is unbalanced or there is no pot). */
public record PoolMemberResponse(long personId, String name, String share, boolean retired, String cut) {
}

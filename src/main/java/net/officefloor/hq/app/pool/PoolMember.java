package net.officefloor.hq.app.pool;

/** A member of a pool: the person (by id, and their name now), their share percent (two decimals),
 *  whether they are retired (a retirement date set, whatever the date) and their cut of the sheet's pot (two
 *  decimals; null when the pool is not balanced or there is no pot). */
public record PoolMember(long personId, String name, String share, boolean retired, String cut) {
}

package net.officefloor.hq.app.pool;

/**
 * A person in a pool with their share percent (two decimals); retired when the person has a retirement date set; and
 * their cut of the sheet's pot (two decimals), or "" when the pool is not balanced or there is no pot.
 */
public record PoolMember(long person, String name, String share, boolean retired, String cut) {
}

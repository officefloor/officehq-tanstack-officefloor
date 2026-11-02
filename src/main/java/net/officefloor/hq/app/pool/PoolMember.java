package net.officefloor.hq.app.pool;

/**
 * A member of a pool with their share percent (two decimals): a person (memberPool null), retired when the person has
 * a retirement date set, or another pool (memberPool its name, person 0, never retired); the membership's from-date and
 * to-date ("" when none) and whether they include the sheet's today (a member outside them is left out entirely); and
 * their cut of the pool's pot (two decimals), or "" when the pool is not balanced or has no pot, or the member is
 * outside their dates.
 */
public record PoolMember(long person, String memberPool, String name, String share, boolean retired, String from,
        String to, boolean current, String cut) {
}

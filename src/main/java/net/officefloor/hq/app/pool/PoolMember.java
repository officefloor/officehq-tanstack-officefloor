package net.officefloor.hq.app.pool;

/** A member of a pool: a person (by id, and their name now) or another pool (by name: {@code pool}, with
 *  {@code personId} null and {@code name} the pool's name), their share percent (two decimals), whether they are
 *  retired (a person with a retirement date set, whatever the date; never a pool), their cut of the pool's pot
 *  (two decimals; null when the pool is not balanced or has no pot, or the member is not current), the
 *  membership's from-date and to-date (yyyy-mm-dd; null when not given) and whether those dates include the
 *  sheet's today (a member that is not current is left out of the pool: its total share, balance and cuts). */
public record PoolMember(Long personId, String pool, String name, String share, boolean retired, String cut,
        String from, String to, boolean current) {
}

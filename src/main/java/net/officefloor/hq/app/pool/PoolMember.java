package net.officefloor.hq.app.pool;

import java.util.Map;

/** A member of a pool: a person (by id, and their name now) or another pool (by name: {@code pool}, with
 *  {@code personId} null and {@code name} the pool's name), their share percent (two decimals), whether they are
 *  retired (a person with a retirement date set, whatever the date; never a pool), their cut of the pool's pot
 *  (two decimals; null when the pool is not balanced or has no pot, or the member is not current), the
 *  membership's from-date and to-date (yyyy-mm-dd; null when not given) and whether those dates include the
 *  sheet's today (a member that is not current is left out of the pool: its total share, balance and cuts),
 *  and, for every period whose to-date is on or before the sheet's today, by period name in from-date order, the
 *  member's cut as it stood at the end of that period (the period's to-date as the sheet's today; null when it had
 *  none then). */
public record PoolMember(Long personId, String pool, String name, String share, boolean retired, String cut,
        String from, String to, boolean current, Map<String, String> periodCuts) {

    public PoolMember(Long personId, String pool, String name, String share, boolean retired, String cut,
            String from, String to, boolean current) {
        this(personId, pool, name, share, retired, cut, from, to, current, Map.of());
    }

    public PoolMember withPeriodCuts(Map<String, String> periodCuts) {
        return new PoolMember(personId, pool, name, share, retired, cut, from, to, current, periodCuts);
    }

    /** The member's key within its pool: its person's id, or pool:<name> for a pool. */
    public String key() {
        return pool == null ? String.valueOf(personId) : "pool:" + pool;
    }
}

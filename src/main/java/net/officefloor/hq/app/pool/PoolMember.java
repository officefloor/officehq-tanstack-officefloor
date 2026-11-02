package net.officefloor.hq.app.pool;

import java.util.Map;

/**
 * A member of a pool with their share percent (two decimals): a person (memberPool null), retired when the person has
 * a retirement date set, or another pool (memberPool its name, person 0, never retired); the membership's from-date and
 * to-date ("" when none) and whether they include the sheet's today (a member outside them is left out entirely); and
 * their cut of the pool's pot (two decimals), or "" when the pool is not balanced or has no pot, or the member is
 * outside their dates; and, by the name of every period whose to-date is on or before the sheet's today, their cut as
 * it stood at the end of that period (the pool, its members, the pot and the holds as they were then, with that day as
 * the sheet's today), "" when there was none; a closed or soft-closed period (ended or not) instead has their cut
 * fixed when it was closed, "" when they were not then in the pool.
 */
public record PoolMember(long person, String memberPool, String name, String share, boolean retired, String from,
        String to, boolean current, String cut, Map<String, String> cuts) {

    public PoolMember(long person, String memberPool, String name, String share, boolean retired, String from,
            String to, boolean current, String cut) {
        this(person, memberPool, name, share, retired, from, to, current, cut, Map.of());
    }

    /** Who the member is within its pool: the person's id, or pool:<name>. */
    public String key() {
        return memberPool != null ? "pool:" + memberPool : String.valueOf(person);
    }
}

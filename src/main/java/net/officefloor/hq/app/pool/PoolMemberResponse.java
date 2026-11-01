package net.officefloor.hq.app.pool;

import java.util.Map;

/** A pool's member as the screen sees it: the person (or, for a member that is a pool, null and the
 *  pool's name in `pool`), their share (two decimals), whether retired, and their cut of the pool's pot
 *  (two decimals; null when the pool is unbalanced or has no pot, or the member is out of their dates),
 *  their from- and to-dates (null when none) and whether those include the sheet's today, and their
 *  cut at the end of each period whose to-date is on or before the sheet's today, by period name in
 *  the periods' order (null when they had none then). */
public record PoolMemberResponse(Long personId, String name, String share, boolean retired, String cut, String pool,
        String from, String to, boolean current, Map<String, String> cuts) {

    public PoolMemberResponse(Long personId, String name, String share, boolean retired, String cut, String pool,
            String from, String to, boolean current) {
        this(personId, name, share, retired, cut, pool, from, to, current, Map.of());
    }

    /** The member with their cuts at the end of each period. */
    public PoolMemberResponse withCuts(Map<String, String> periodCuts) {
        return new PoolMemberResponse(personId, name, share, retired, cut, pool, from, to, current, periodCuts);
    }

    /** The member's key within its pool: the person's id, or pool:<name> for a member pool. */
    public String key() {
        return pool != null ? "pool:" + pool : String.valueOf(personId);
    }
}

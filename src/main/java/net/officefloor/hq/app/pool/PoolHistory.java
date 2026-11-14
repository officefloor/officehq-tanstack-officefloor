package net.officefloor.hq.app.pool;

import java.util.List;

/**
 * The sheet's pools as they stood at the end of each period whose to-date is on or before the sheet's today, earliest
 * first: the pools, their members, the pot and the holds as they were then, with that day as the sheet's today.
 */
public record PoolHistory(List<Ended> periods) {

    /** The pools as they stood at the end of the named period. */
    public record Ended(String period, Pools pools) {
    }
}

package net.officefloor.hq.app.pool;

import java.util.List;
import java.util.Map;

/**
 * The sheet's pools' cuts for each period that has them, in the periods' order: fixed when the period was closed or
 * soft-closed, else (an open period whose to-date is on or before the sheet's today) as the pools, their members, the
 * pot and the holds stood at its end, with that day as the sheet's today.
 */
public record PoolHistory(List<Ended> periods) {

    /**
     * The cuts for the named period: {@code fixed} (by {@link PoolRepository#cutKey}) when it is closed or
     * soft-closed, else worked out from the pools as they stood at its end ({@code pools}).
     */
    public record Ended(String period, Pools pools, Map<String, String> fixed) {
    }
}

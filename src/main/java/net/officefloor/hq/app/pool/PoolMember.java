package net.officefloor.hq.app.pool;

import java.util.Map;

/** A member of a pool, with their share percent (two decimals) and their cut of the pool's pot (two
 *  decimals; "" when the pool is unbalanced or has no pot). The member is a person ({@code personId},
 *  {@code poolId} null; {@code retired} when they are) or another pool ({@code poolId}, {@code personId}
 *  null; {@code name} the pool's name; never retired). {@code from} and {@code to} are the dates the
 *  membership runs (null: no bound); {@code current} when they include the sheet's today — a member that
 *  is not is left out of the total share and gets no cut (""). {@code periodCuts}: for every period whose
 *  to-date is on or before the sheet's today (by from-date), the member's cut as it stood at the end of it
 *  ("" when there was none); for a closed or soft-closed period (shown whatever its to-date), the cut fixed
 *  when it was closed. */
public record PoolMember(Long personId, Long poolId, String name, String share, boolean retired, String from,
        String to, boolean current, String cut, Map<String, String> periodCuts) {

    public PoolMember withCut(String cut) {
        return new PoolMember(personId, poolId, name, share, retired, from, to, current, cut, periodCuts);
    }

    public PoolMember withPeriodCuts(Map<String, String> periodCuts) {
        return new PoolMember(personId, poolId, name, share, retired, from, to, current, cut, periodCuts);
    }
}

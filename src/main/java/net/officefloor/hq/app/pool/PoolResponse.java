package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as shown: its members, the total of their shares (a retired member's share counted as given) and whether
 *  that total is exactly 100.00. */
public record PoolResponse(String name, String totalShare, boolean balanced, List<Member> members) {

    /** A member: the person's id and name (or, for a pool that is a member, personId null, memberPool and name its
     *  name), their share (two decimals), whether a retirement date is set, whether the membership's dates include the sheet's today (when not, the member
     *  is left out of the total and has no cut) and their cut of the pool's pot (two
     *  decimals; "" when the pool is not balanced or has no pot). */
    public record Member(String personId, String memberPool, String name, String share, boolean retired, boolean current,
            String cut) {
    }
}

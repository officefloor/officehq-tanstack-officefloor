package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as the screen shows it: its name, its members, the total of their shares (a retired member's counting as
 *  given) and whether that total is exactly 100.00. */
public record PoolResponse(long id, String name, String totalShare, boolean balanced, List<Member> members) {

    /** A member: the person (or the member pool, the other null), its name, their share and their cut of the sheet's pot ("" when the pool is not
     *  balanced or there is no pot). */
    public record Member(Long personId, Long poolId, String name, String share, String cut) {
    }
}

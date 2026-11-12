package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as the front end reads it: its members, the total of their shares (a retired member's counts as
 *  0.00) and whether that total is exactly 100.00. Shares and the total have two decimals. */
public record PoolResponse(long id, String name, List<MemberResponse> members, String totalShare, boolean balanced) {

    /** A member of a pool: the person or the member pool (the other null), its name, its share, and its cut of
     *  the pool's pot (two decimals), null when the pool is not balanced or has no pot. A pool that is a member
     *  of none has the sheet's pot; a member pool's pot is its cut in the pool above. The membership's from- and to-dates, null for no bound; a member whose dates do
     *  not include the sheet's today counts in neither the total nor the balance and has no cut. */
    public record MemberResponse(Long personId, Long memberPoolId, String name, String share, boolean retired,
            String cut, String from, String to) {
    }
}

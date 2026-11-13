package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as the front end reads it: its members, the total of their shares (a retired member's counts as
 *  given) and whether that total is exactly 100.00. Shares and the total have two decimals. {@code crew} for a
 *  crew shown as a pool (its id the negated crew id): its members are the crew's, and none is added to it here. */
public record PoolResponse(long id, String name, List<MemberResponse> members, String totalShare, boolean balanced,
        boolean crew) {

    /** A member of a pool: the person or the member pool (the other null), its name, its share, and its cut of
     *  the pool's pot (two decimals), null when the pool is not balanced, has no pot or every member is retired. A pool that is a member
     *  of none has the sheet's pot; a member pool's pot is its cut in the pool above. The membership's from- and to-dates, null for no bound; a member whose dates do
     *  not include the sheet's today counts in neither the total nor the balance and has no cut. */
    public record MemberResponse(Long personId, Long memberPoolId, String name, String share, boolean retired,
            String cut, String from, String to, List<PeriodCut> periodCuts) {
    }

    /** A member's cut as it stood at the end of a period whose to-date is on or before the sheet's today (the
     *  pool, its members, the pot and the holds as they were then, with that day as the sheet's today), null when
     *  there was none; for a closed or soft-closed period, whatever its to-date, the cut fixed when it was closed. In the periods' order. */
    public record PeriodCut(String period, String cut) {
    }
}

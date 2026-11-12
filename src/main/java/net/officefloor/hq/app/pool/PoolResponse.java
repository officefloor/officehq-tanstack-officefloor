package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as the front end reads it: its members, the total of their shares (a retired member's counts as
 *  0.00) and whether that total is exactly 100.00. Shares and the total have two decimals. */
public record PoolResponse(long id, String name, List<MemberResponse> members, String totalShare, boolean balanced) {

    /** A member of a pool: the person, their share, and their cut of the sheet's pot (two decimals), null when
     *  the pool is not balanced or the sheet has no pot. */
    public record MemberResponse(long personId, String name, String share, boolean retired, String cut) {
    }
}

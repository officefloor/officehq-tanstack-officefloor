package net.officefloor.hq.app.pool;

import java.util.List;

/**
 * A pool: its name, its members in the order added, the total of their shares (two decimals; a retired member's share
 * counts as 0.00) and whether that total is exactly 100.00.
 */
public record PoolResponse(String name, List<Member> members, String totalShare, boolean balanced) {

    /**
     * A member: the person's id and name, their share (two decimals), whether they are retired, and their cut of the
     * sheet's pot (two decimals; null while the pool is unbalanced or the sheet has no pot).
     */
    public record Member(long personId, String name, String share, boolean retired, String cut) {

        Member withCut(String cut) {
            return new Member(personId, name, share, retired, cut);
        }
    }
}

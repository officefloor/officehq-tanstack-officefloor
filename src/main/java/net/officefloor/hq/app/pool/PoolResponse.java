package net.officefloor.hq.app.pool;

import java.util.List;

/**
 * A pool as the screen sees it: its name, its members, the total of their shares (a retired member's counting 0.00)
 * with two decimals, and whether that total is exactly 100.00.
 */
public record PoolResponse(String name, List<Member> members, String totalShare, boolean balanced) {

    /** A member: the person's id and name, their share with two decimals, and whether they are retired. */
    public record Member(long personId, String name, String share, boolean retired) {
    }
}

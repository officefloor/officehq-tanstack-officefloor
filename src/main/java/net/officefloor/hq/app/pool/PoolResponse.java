package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as shown: its members, the total of their shares (a retired member's counting 0.00) and whether
 *  that total is exactly 100.00. */
public record PoolResponse(String name, String totalShare, boolean balanced, List<Member> members) {

    /** A member: the person's id and name, their share (two decimals) and whether a retirement date is set. */
    public record Member(String personId, String name, String share, boolean retired) {
    }
}

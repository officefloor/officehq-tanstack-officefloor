package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as the screen sees it: its name, its members in the order added, the total of their shares
 *  (two decimals; a retired member's counts 0.00), whether that total is exactly 100.00 and whether it
 *  is a crew's (its members the crew's lead and members, added through the crew, not the pool). */
public record PoolResponse(String name, List<PoolMemberResponse> members, String totalShare, boolean balanced,
        boolean crew) {
}

package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool as the screen sees it: its name, its members in the order added, the total of their shares
 *  (two decimals; a retired member's counts 0.00) and whether that total is exactly 100.00. */
public record PoolResponse(String name, List<PoolMemberResponse> members, String totalShare, boolean balanced) {
}

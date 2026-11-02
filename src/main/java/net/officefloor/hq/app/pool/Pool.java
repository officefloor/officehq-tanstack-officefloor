package net.officefloor.hq.app.pool;

import java.util.List;

/**
 * One of the sheet's pools: its members, the total of their shares (a retired member's counting as given) and whether that
 * total is exactly 100.00.
 */
public record Pool(String name, List<PoolMember> members, String totalShare, boolean balanced) {
}

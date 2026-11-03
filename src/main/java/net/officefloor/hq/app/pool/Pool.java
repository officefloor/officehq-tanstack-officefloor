package net.officefloor.hq.app.pool;

import java.util.List;

/** A sheet's pool: its name, its members (people and pools) in the order they were added, the total of their shares (a
 *  retired member's share counted as given, one whose dates do not include the sheet's today, counting as 0.00; two decimals) and whether that total is exactly 100.00. */
public record Pool(String name, List<PoolMember> members, String totalShare, boolean balanced) {
}

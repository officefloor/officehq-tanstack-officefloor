package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool on a sheet, its members (in the order they were added), the total of their shares (a retired
 *  member's counting as 0.00) and whether that total is exactly 100.00. */
public record Pool(long id, String sheetId, String name, List<PoolMember> members, String totalShare,
        boolean balanced) {
}

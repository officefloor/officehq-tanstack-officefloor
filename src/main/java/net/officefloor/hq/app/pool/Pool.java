package net.officefloor.hq.app.pool;

import java.util.List;

/** A pool on a sheet, its members (in the order they were added), the total of their shares (a retired
 *  member's counting as given; one whose dates do not include the sheet's today counting as 0.00) and whether that total is exactly 100.00. {@code parentId} is the pool it is a
 *  member of (null: none); its pot is then its cut there, else the sheet's pot. {@code crew} when it is a
 *  crew's pool, its members the crew's (none can be added to it, nor it to another pool). */
public record Pool(long id, String sheetId, String name, List<PoolMember> members, String totalShare,
        boolean balanced, Long parentId, boolean crew) {
}

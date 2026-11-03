package net.officefloor.hq.app.pool;

import java.util.List;

/** A sheet's pool: its name, its members (people and pools) in the order they were added, the total of their shares (a
 *  retired member's share counted as given, one whose dates do not include the sheet's today, counting as 0.00; two decimals),
 *  whether that total is exactly 100.00 and its place in the order the sheet's pools were added (0 for the first). */
public record Pool(String name, List<PoolMember> members, String totalShare, boolean balanced, int added) {

    public Pool withAdded(int added) {
        return new Pool(name, members, totalShare, balanced, added);
    }
}

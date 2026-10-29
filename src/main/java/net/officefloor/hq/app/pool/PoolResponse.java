package net.officefloor.hq.app.pool;

import java.util.List;

/**
 * A pool as the screen sees it: its name, its members, the total of their shares (a retired member's counting as given)
 * with two decimals, and whether that total is exactly 100.00.
 */
public record PoolResponse(String name, List<Member> members, String totalShare, boolean balanced) {

    /**
     * A member: the person's id and name, their share with two decimals, whether they are retired, and their cut of the
     * pot with two decimals ("" when the pool is not balanced or there is no pot). A member that is another pool has
     * personId 0, its name as name and as pool, and is never retired; pool is null for a person.
     */
    public record Member(long personId, String name, String share, boolean retired, String cut, String pool) {
    }
}

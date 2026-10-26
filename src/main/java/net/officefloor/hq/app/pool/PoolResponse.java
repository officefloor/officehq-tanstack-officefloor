package net.officefloor.hq.app.pool;

import java.util.List;

/**
 * A pool: its name, its members in the order added, the total of their shares (two decimals; a retired member's share
 * counts as 0.00) and whether that total is exactly 100.00.
 */
public record PoolResponse(String name, List<Member> members, String totalShare, boolean balanced) {

    /**
     * A member: a person (their id) or another pool (personId null, {@code pool} its name), its name, its share (two
     * decimals), whether the person is retired, whether they have a hold in force on the sheet's today, and its cut of the pool's pot (two decimals; null while the pool is
     * unbalanced or has no pot).
     */
    public record Member(Long personId, String pool, String name, String share, boolean retired, boolean held,
            String cut) {

        Member withCut(String cut) {
            return new Member(personId, pool, name, share, retired, held, cut);
        }

        /** Whether the member takes a cut of the pool's pot: neither retired nor held. */
        boolean hasCut() {
            return !retired && !held;
        }
    }
}

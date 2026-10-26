package net.officefloor.hq.app.pool;

import java.util.List;

/**
 * A pool: its name, its members in the order added, the total of their shares (two decimals; a retired member's share
 * counts as 0.00, and one out of its membership dates is left out) and whether that total is exactly 100.00.
 */
public record PoolResponse(String name, List<Member> members, String totalShare, boolean balanced) {

    /**
     * A member: a person (their id) or another pool (personId null, {@code pool} its name), its name, its share (two
     * decimals), whether the person is retired, whether they have a hold in force on the sheet's today, whether its membership dates leave out the sheet's
     * today ({@code out}: no cut at all, not counted in the total share), and its cut of the pool's pot (two decimals; null while the pool is
     * unbalanced or has no pot).
     */
    public record Member(Long personId, String pool, String name, String share, boolean retired, boolean held,
            boolean out, String cut) {

        Member withCut(String cut) {
            return new Member(personId, pool, name, share, retired, held, out, cut);
        }

        /** Whether the member takes a cut of the pool's pot: neither retired, held nor out of its membership dates. */
        boolean hasCut() {
            return !retired && !held && !out;
        }
    }
}

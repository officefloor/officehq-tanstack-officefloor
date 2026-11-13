package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.List;

/**
 * The sheet's pools, in the order they were added, and the sheet's pot (null: none): the pot of each pool that is a
 * member of none, shared out among its members; a pool that is a member of another has its cut there as its pot.
 */
public record Pools(List<Pool> list, BigDecimal pot) {

    /** The pool this one is a member of, or null. */
    public Pool parentOf(long poolId) {
        return list.stream().filter(p -> p.members().stream().anyMatch(m -> m.poolId() != null && m.poolId() == poolId))
                .findFirst().orElse(null);
    }

    /** The pool's pot: the sheet's when it is a member of no pool, else its cut in the pool above (null: none). */
    public BigDecimal potOf(Pool pool) {
        return potOf(pool, 0);
    }

    private BigDecimal potOf(Pool pool, int depth) {
        Pool parent = parentOf(pool.id());
        if (parent == null) {
            return pot;
        }
        if (depth > list.size()) {
            return null; // a cycle, which validation never lets in
        }
        List<BigDecimal> cuts = PoolResponse.cuts(parent, potOf(parent, depth + 1));
        if (cuts == null) {
            return null;
        }
        for (int i = 0; i < parent.members().size(); i++) {
            Long id = parent.members().get(i).poolId();
            if (pool.id().equals(id)) {
                return cuts.get(i);
            }
        }
        return null;
    }

    /** The number of levels of pools from this one down (1: no pool members). */
    public int height(long poolId) {
        return height(poolId, 0);
    }

    private int height(long poolId, int guard) {
        Pool pool = find(poolId);
        if (pool == null || guard > list.size()) {
            return 1;
        }
        int below = 0;
        for (PoolMember m : pool.members()) {
            if (m.poolId() != null) {
                below = Math.max(below, height(m.poolId(), guard + 1));
            }
        }
        return 1 + below;
    }

    /** The number of pools from this one up to the one that is a member of none (1: a member of none). */
    public int level(long poolId) {
        int level = 1;
        Pool parent = parentOf(poolId);
        while (parent != null && level <= list.size()) {
            level++;
            parent = parentOf(parent.id());
        }
        return level;
    }

    /** Whether {@code poolId} is {@code ancestor} or lies under it through any chain of pool members. */
    public boolean within(long poolId, long ancestor) {
        Long at = poolId;
        for (int i = 0; at != null && i <= list.size(); i++) {
            if (at == ancestor) {
                return true;
            }
            Pool parent = parentOf(at);
            at = parent == null ? null : parent.id();
        }
        return false;
    }

    /** The pool with this id, or null. */
    public Pool find(long poolId) {
        return list.stream().filter(p -> p.id() == poolId).findFirst().orElse(null);
    }
}

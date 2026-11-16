package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member of a pool: a person (personId and name) or another pool (pool, its name; personId null), the share percent,
 *  whether the person is retired (then it counts as 0.00; a pool never is), and the cut of the pool's pot (two
 *  decimals; null when the pool is not balanced or has no pot). */
public record PoolMember(Long personId, String name, String pool, BigDecimal share, boolean retired, String cut) {

    /** The member with their cut of the pot. */
    public PoolMember withCut(BigDecimal cut) {
        return new PoolMember(personId, name, pool, share, retired, cut == null ? null : cut.toPlainString());
    }
}

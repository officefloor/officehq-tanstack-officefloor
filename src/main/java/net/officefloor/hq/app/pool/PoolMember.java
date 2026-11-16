package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member of a pool: a person (personId and name) or another pool (pool, its name; personId null), the share percent,
 *  whether the person is retired (then it counts as 0.00; a pool never is), and the cut of the pool's pot (two
 *  decimals; null when the pool is not balanced or has no pot). A membership may carry a from-date and a to-date
 *  (yyyy-MM-dd, null for none); active is whether they include the sheet's today: an inactive member is left out
 *  entirely (not in the total, no cut). */
public record PoolMember(Long personId, String name, String pool, BigDecimal share, boolean retired, String cut,
        String from, String to, boolean active) {

    /** The member with their cut of the pot (an inactive member never has one). */
    public PoolMember withCut(BigDecimal cut) {
        return new PoolMember(personId, name, pool, share, retired,
                cut == null || !active ? null : cut.toPlainString(), from, to, active);
    }
}

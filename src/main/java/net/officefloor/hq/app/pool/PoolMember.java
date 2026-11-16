package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member of a pool: the person, their share percent, whether they are retired (then it counts as 0.00), and their
 *  cut of the sheet's pot (two decimals; null when the pool is not balanced or the sheet has no pot). */
public record PoolMember(long personId, String name, BigDecimal share, boolean retired, String cut) {

    /** The member with their cut of the pot. */
    public PoolMember withCut(BigDecimal cut) {
        return new PoolMember(personId, name, share, retired, cut == null ? null : cut.toPlainString());
    }
}

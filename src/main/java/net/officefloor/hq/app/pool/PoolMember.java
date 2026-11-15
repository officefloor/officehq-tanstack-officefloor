package net.officefloor.hq.app.pool;

/** A member of a pool, with their share percent (two decimals) and their cut of the pool's pot (two
 *  decimals; "" when the pool is unbalanced or has no pot). The member is a person ({@code personId},
 *  {@code poolId} null; {@code retired} when they are) or another pool ({@code poolId}, {@code personId}
 *  null; {@code name} the pool's name; never retired). */
public record PoolMember(Long personId, Long poolId, String name, String share, boolean retired, String cut) {

    public PoolMember withCut(String cut) {
        return new PoolMember(personId, poolId, name, share, retired, cut);
    }
}

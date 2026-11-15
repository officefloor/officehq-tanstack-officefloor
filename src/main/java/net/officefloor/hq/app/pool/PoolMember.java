package net.officefloor.hq.app.pool;

/** A person in a pool, with their share percent (two decimals), whether they are retired and their cut
 *  of the sheet's pot (two decimals; "" when the pool is unbalanced or there is no pot). */
public record PoolMember(long personId, String name, String share, boolean retired, String cut) {

    public PoolMember withCut(String cut) {
        return new PoolMember(personId, name, share, retired, cut);
    }
}

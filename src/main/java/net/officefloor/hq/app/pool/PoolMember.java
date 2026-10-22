package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member of a pool with their share percent (more than 0.00, at most 100.00): a person, or another pool
 *  (memberPoolId; personId then 0, and name the pool's name). */
public record PoolMember(long poolId, String sheetId, String pool, long personId, String name, BigDecimal share,
        String by, Long memberPoolId) {

    public PoolMember(long poolId, String sheetId, String pool, long personId, String name, BigDecimal share,
            String by) {
        this(poolId, sheetId, pool, personId, name, share, by, null);
    }

    public boolean isPool() {
        return memberPoolId != null;
    }
}

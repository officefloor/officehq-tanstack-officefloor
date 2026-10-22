package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A member of a pool with their share percent (more than 0.00, at most 100.00): a person, or another pool
 *  (memberPoolId; personId then 0, and name the pool's name); from and to date the membership (each null when
 *  open, both inclusive). */
public record PoolMember(long poolId, String sheetId, String pool, long personId, String name, BigDecimal share,
        String by, Long memberPoolId, LocalDate from, LocalDate to) {

    public PoolMember(long poolId, String sheetId, String pool, long personId, String name, BigDecimal share,
            String by) {
        this(poolId, sheetId, pool, personId, name, share, by, null, null, null);
    }

    public boolean isPool() {
        return memberPoolId != null;
    }

    /** Whether the membership's dates include this day (always, when there is no day). */
    public boolean covers(LocalDate on) {
        return on == null || ((from == null || !from.isAfter(on)) && (to == null || !to.isBefore(on)));
    }

    /** The same member under another name. */
    public PoolMember named(String name) {
        return new PoolMember(poolId, sheetId, pool, personId, name, share, by, memberPoolId, from, to);
    }
}

package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/**
 * A member of a pool: a person ({@code personId}) or another pool of the sheet ({@code poolId}), exactly one set, with
 * its name, its share percent (two decimals), whether it is a person with a retirement date set and whether it is a
 * person with a hold in force on the sheet's today (a hold whose dates include today).
 */
public record PoolMember(Long personId, Long poolId, String name, BigDecimal share, boolean retired, boolean held) {

    public PoolMember(Long personId, Long poolId, String name, BigDecimal share, boolean retired) {
        this(personId, poolId, name, share, retired, false);
    }

    /** Whether the member gets a cut: it is neither retired nor held. */
    public boolean cut() {
        return !retired && !held;
    }
}

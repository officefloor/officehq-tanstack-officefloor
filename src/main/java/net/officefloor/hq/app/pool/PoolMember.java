package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A member of a pool: a person ({@code personId}) or another pool of the sheet ({@code poolId}), exactly one set, with
 * its name, its share percent (two decimals), whether it is a person with a retirement date set and whether it is a
 * person with a hold in force on the sheet's today (a hold whose dates include today), its membership's optional
 * from-date and to-date (null: no start / no end) and whether those dates leave out the sheet's today ({@code out}): an
 * out member is left out of its pool entirely.
 */
public record PoolMember(Long personId, Long poolId, String name, BigDecimal share, boolean retired, boolean held,
        LocalDate from, LocalDate to, boolean out) {

    public PoolMember(Long personId, Long poolId, String name, BigDecimal share, boolean retired) {
        this(personId, poolId, name, share, retired, false, null, null, false);
    }

    /** Whether the member gets a cut: it is in the pool on the sheet's today, and neither retired nor held. */
    public boolean cut() {
        return !out && !retired && !held;
    }

    /** Whether the dates {@code from} to {@code to} (null: no start / no end) include {@code today} (null: always). */
    public static boolean includes(LocalDate from, LocalDate to, LocalDate today) {
        return today == null || ((from == null || !from.isAfter(today)) && (to == null || !to.isBefore(today)));
    }
}

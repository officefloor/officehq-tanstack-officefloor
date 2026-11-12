package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A member to add, validated: the pool, the person or the member pool (the other null), the member's name,
 *  their share, the dates the membership counts from and to (null for no bound), and the user adding them. */
public record MemberInput(PoolStore.Pool pool, Long personId, Long memberPoolId, String personName, BigDecimal share,
        LocalDate from, LocalDate to, String user) {
}

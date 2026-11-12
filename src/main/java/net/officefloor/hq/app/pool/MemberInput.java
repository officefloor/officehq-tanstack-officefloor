package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member to add, validated: the pool, the person or the member pool (the other null), the member's name,
 *  their share, and the user adding them. */
public record MemberInput(PoolStore.Pool pool, Long personId, Long memberPoolId, String personName, BigDecimal share,
        String user) {
}

package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A pool member request, validated: a share more than 0.00 and at most 100.00, two decimals. */
public record PoolMemberInput(String pool, long personId, BigDecimal share) {
}

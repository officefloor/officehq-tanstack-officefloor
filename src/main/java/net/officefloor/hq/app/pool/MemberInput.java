package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member to add, validated: the pool, the person, their share, and the user adding them. */
public record MemberInput(PoolStore.Pool pool, long personId, String personName, BigDecimal share, String user) {
}

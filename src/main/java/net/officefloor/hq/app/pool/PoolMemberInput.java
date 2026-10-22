package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A validated pool member add: who is adding it, the person and their share percent (two decimals). */
public record PoolMemberInput(String user, long personId, BigDecimal share) {
}

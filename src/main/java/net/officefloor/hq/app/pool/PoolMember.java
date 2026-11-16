package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member of a pool: the person, their share percent, and whether they are retired (then it counts as 0.00). */
public record PoolMember(long personId, String name, BigDecimal share, boolean retired) {
}

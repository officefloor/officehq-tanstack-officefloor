package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member of a pool: the person, their share percent (two decimals) and whether they have a retirement date set. */
public record PoolMember(long personId, String name, BigDecimal share, boolean retired) {
}

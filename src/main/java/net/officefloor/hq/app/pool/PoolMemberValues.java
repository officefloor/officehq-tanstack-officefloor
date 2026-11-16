package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member to add once validated: the pool's name, the person (id and name), the share (two decimals), and who adds
 *  it. */
public record PoolMemberValues(String pool, long personId, String name, BigDecimal share, String by) {
}

package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member to add once validated: the pool's name, the person (id and name) or the member pool (personId null, name
 *  the pool's), the share (two decimals), and who adds it. */
public record PoolMemberValues(String pool, Long personId, String name, boolean isPool, BigDecimal share, String by) {
}

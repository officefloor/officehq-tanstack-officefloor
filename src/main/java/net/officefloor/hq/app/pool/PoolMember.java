package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A person added to a pool with a share percent (two decimals), added by `by`. */
public record PoolMember(String sheetId, String pool, long personId, String name, BigDecimal share, String by) {
}

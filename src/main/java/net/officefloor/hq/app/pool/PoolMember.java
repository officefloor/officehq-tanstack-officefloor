package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A person in a pool with a share percent. A retired person's share counts as 0.00 toward the pool's total. */
public record PoolMember(String sheetId, String pool, long personId, String name, BigDecimal share, boolean retired) {
}

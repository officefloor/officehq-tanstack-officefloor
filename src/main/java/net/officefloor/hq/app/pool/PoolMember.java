package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A person in a pool with their share percent (more than 0.00, at most 100.00). */
public record PoolMember(long poolId, String sheetId, String pool, long personId, String name, BigDecimal share,
        String by) {
}

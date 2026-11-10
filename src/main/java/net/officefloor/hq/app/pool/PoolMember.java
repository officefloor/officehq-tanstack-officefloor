package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/**
 * A member of a pool with a share percent: a person (personId set, memberPool null), or another pool
 * (memberPool its name, personId null; name the pool's name). A retired person's share counts as 0.00
 * toward the pool's total.
 */
public record PoolMember(String sheetId, String pool, Long personId, String memberPool, String name, BigDecimal share,
        boolean retired) {
}

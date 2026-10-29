package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/**
 * A member of a pool and their share percent: a person (by id and name, memberPool null), or another pool (memberPool
 * and name its name, personId 0).
 */
public record PoolMember(String sheetId, String pool, long personId, String name, BigDecimal share, String memberPool) {
}

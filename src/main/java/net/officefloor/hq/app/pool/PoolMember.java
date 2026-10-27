package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member of a pool: the person (by id and name) and their share percent. */
public record PoolMember(String sheetId, String pool, long personId, String name, BigDecimal share) {
}

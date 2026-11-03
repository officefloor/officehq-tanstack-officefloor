package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A validated member form: the pool (by name), the person (by id) and the share percent (two decimals). */
public record PoolMemberInput(String pool, long personId, BigDecimal share) {
}

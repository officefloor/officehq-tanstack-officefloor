package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A validated member to add: the pool's name, the person's id and their share (two decimals, 0.00 < share <= 100.00). */
public record PoolMemberInput(String pool, long personId, BigDecimal share) {
}

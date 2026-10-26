package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/**
 * A validated member to add: the pool's name, the member — a person's id, or else (personId null) the name of another
 * pool — and their share (two decimals, 0.00 < share <= 100.00).
 */
public record PoolMemberInput(String pool, Long personId, String memberPool, BigDecimal share) {
}

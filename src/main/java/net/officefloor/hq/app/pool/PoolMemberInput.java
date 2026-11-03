package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A validated member form: the pool (by name), the member (a person by id, or another pool by name; exactly one
 *  of them) and the share percent (two decimals). */
public record PoolMemberInput(String pool, Long personId, String memberPool, BigDecimal share) {
}

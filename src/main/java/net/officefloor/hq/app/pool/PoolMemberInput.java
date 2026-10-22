package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A validated pool member add: who is adding it, the person or the pool (memberPoolId; the other null) and their
 *  share percent (two decimals). */
public record PoolMemberInput(String user, Long personId, BigDecimal share, Long memberPoolId) {
}

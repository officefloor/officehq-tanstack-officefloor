package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A pool member entry that passed validation: the person's id, or the member pool's id (the other null), and the
 *  share, at two decimals. */
public record ValidPoolMember(Long personId, Long memberPoolId, BigDecimal share) {
}

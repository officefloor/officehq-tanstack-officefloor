package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A pool member entry that passed validation: the person's id and their share, at two decimals. */
public record ValidPoolMember(long personId, BigDecimal share) {
}

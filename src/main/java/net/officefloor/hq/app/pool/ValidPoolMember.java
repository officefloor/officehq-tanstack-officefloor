package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member that passed validation: a person of the sheet not yet in the pool, with a share in (0.00, 100.00]. */
public record ValidPoolMember(String pool, long personId, String personName, BigDecimal share, String user) {
}

package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** A member that passed validation: a person of the sheet not yet in the pool (memberPool null), or another pool of
 *  the sheet (personId null) that may nest there, with a share in (0.00, 100.00]. */
public record ValidPoolMember(String pool, Long personId, String memberPool, String memberName, BigDecimal share,
        String user) {
}

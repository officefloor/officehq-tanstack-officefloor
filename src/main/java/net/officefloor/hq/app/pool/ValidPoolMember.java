package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A pool member entry that passed validation: the person's id, or the member pool's id (the other null), the
 *  share, at two decimals, and the optional dates the membership runs from and to. */
public record ValidPoolMember(Long personId, Long memberPoolId, BigDecimal share, LocalDate from, LocalDate to) {
}

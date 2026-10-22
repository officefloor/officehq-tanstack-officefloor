package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated pool member add: who is adding it, the person or the pool (memberPoolId; the other null), their
 *  share percent (two decimals) and the membership's from- and to-dates (each null when not given; to not before
 *  from). */
public record PoolMemberInput(String user, Long personId, BigDecimal share, Long memberPoolId, LocalDate from,
        LocalDate to) {
}

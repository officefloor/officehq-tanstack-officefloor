package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A pool member request, validated: a person's id, or another pool's name (memberPool, null for a
 * person), and a share more than 0.00 and at most 100.00, two decimals; optional from- and to-dates, the to-date not before the from-date.
 */
public record PoolMemberInput(String pool, Long personId, String memberPool, BigDecimal share, LocalDate from, LocalDate to) {
}

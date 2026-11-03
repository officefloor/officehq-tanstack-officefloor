package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A validated member form: the pool (by name), the member (a person by id, or another pool by name; exactly one
 *  of them), the share percent (two decimals) and the membership's optional from-date and to-date. */
public record PoolMemberInput(String pool, Long personId, String memberPool, BigDecimal share, LocalDate from,
        LocalDate to) {
}

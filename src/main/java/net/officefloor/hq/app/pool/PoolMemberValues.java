package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A member to add once validated: the pool's name, the person (id and name) or the member pool (personId null, name
 *  the pool's), the share (two decimals), the membership's from-date and to-date (null for none), and who adds it. */
public record PoolMemberValues(String pool, Long personId, String name, boolean isPool, BigDecimal share,
        LocalDate from, LocalDate to, String by) {
}

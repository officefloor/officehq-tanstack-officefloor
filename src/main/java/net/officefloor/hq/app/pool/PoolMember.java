package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** A person (personId), or another pool (memberPool), added to a pool with a share percent (two
 *  decimals), added by `by`, between from and to (each optional, inclusive). `name` is the person's or the
 *  member pool's. */
public record PoolMember(String sheetId, String pool, Long personId, String memberPool, String name, BigDecimal share,
        String by, LocalDate from, LocalDate to) {
}

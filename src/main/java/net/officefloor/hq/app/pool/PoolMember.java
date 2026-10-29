package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * A member of a pool and their share percent: a person (by id and name, memberPool null), or another pool (memberPool
 * and name its name, personId 0); a member from a from-date to a to-date, both inclusive, each null when not given.
 */
public record PoolMember(String sheetId, String pool, long personId, String name, BigDecimal share, String memberPool,
        LocalDate from, LocalDate to) {
}

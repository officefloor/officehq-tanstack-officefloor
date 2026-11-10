package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/**
 * A member of a pool with a share percent: a person (personId set, memberPool null), or another pool
 * (memberPool its name, personId null; name the pool's name). A retired person's share counts as 0.00
 * toward the pool's total. {@code held} is whether a person has a hold in force on the sheet's today: they
 * get no cut, their share being spread over the members with one, though it still counts toward the total.
 */
public record PoolMember(String sheetId, String pool, Long personId, String memberPool, String name, BigDecimal share,
        boolean retired, boolean held) {
}

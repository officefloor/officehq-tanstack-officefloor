package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/**
 * A member of a pool: a person ({@code personId}) or another pool of the sheet ({@code poolId}), exactly one set, with
 * its name, its share percent (two decimals) and whether it is a person with a retirement date set.
 */
public record PoolMember(Long personId, Long poolId, String name, BigDecimal share, boolean retired) {
}

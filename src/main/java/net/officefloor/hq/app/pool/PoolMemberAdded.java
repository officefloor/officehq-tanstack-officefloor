package net.officefloor.hq.app.pool;

import java.math.BigDecimal;

/** The member just added: the pool's name, the person's name and their share. */
public record PoolMemberAdded(String sheetId, String pool, String name, BigDecimal share) {
}

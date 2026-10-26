package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;

/** The member just added: the pool's name, the person's name, their share and the membership's dates (null when not given). */
public record PoolMemberAdded(String sheetId, String pool, String name, BigDecimal share,
        LocalDate from, LocalDate to) {
}

package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.List;

/** A pool of the sheet: its members, the total of their shares (a retired member's counting as given), and whether that
 *  total is exactly 100.00. */
public record Pool(String name, List<PoolMember> members, BigDecimal totalShare, boolean balanced) {
}

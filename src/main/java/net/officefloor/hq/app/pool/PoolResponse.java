package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.List;

/**
 * A pool, as the screen reads it: its members in the order added, the total of their shares (a retired
 * member's counting as 0.00) and whether that total is exactly 100.00.
 */
public record PoolResponse(String name, List<PoolMemberResponse> members, String totalShare, boolean balanced) {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public static PoolResponse from(Pool pool, List<PoolMember> members) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (PoolMember m : members) if (!m.retired()) total = total.add(m.share());
        return new PoolResponse(pool.name(), members.stream().map(PoolMemberResponse::from).toList(),
                total.toPlainString(), total.compareTo(HUNDRED) == 0);
    }
}

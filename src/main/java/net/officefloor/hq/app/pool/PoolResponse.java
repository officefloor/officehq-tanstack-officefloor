package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.List;

/**
 * A pool as the screen reads it: its members' shares with two decimals, the total share (a retired member's share,
 * whatever their retirement date, counts as 0.00) and whether that total is exactly 100.00.
 */
public record PoolResponse(long id, String name, List<Member> members, String totalShare, boolean balanced) {

    public record Member(long personId, String name, String share, boolean retired) {
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public static PoolResponse from(Pool pool) {
        BigDecimal total = pool.members().stream().filter(m -> !m.retired()).map(PoolMember::share)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        return new PoolResponse(pool.id(), pool.name(), pool.members().stream()
                .map(m -> new Member(m.personId(), m.name(), m.share().setScale(2).toPlainString(), m.retired()))
                .toList(), total.toPlainString(), total.compareTo(HUNDRED) == 0);
    }
}

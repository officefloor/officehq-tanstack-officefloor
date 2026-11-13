package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A pool as the screen reads it: its members' shares with two decimals, the total share (a retired member's share,
 * whatever their retirement date, counts as 0.00) and whether that total is exactly 100.00. In a balanced pool, with a
 * pot, each member's cut is the pot times their share divided by 100, rounded half to even to two decimals (a retired
 * member's is 0.00); the remainder (the pot less the cuts added up) goes to the member with the largest share, the
 * earliest added on a tie, so the cuts total exactly the pot. Otherwise a member's cut is "" (none).
 */
public record PoolResponse(long id, String name, List<Member> members, String totalShare, boolean balanced) {

    public record Member(long personId, String name, String share, boolean retired, String cut) {
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public static PoolResponse from(Pool pool, BigDecimal pot) {
        BigDecimal total = pool.members().stream().filter(m -> !m.retired()).map(PoolMember::share)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
        boolean balanced = total.compareTo(HUNDRED) == 0;
        List<String> cuts = balanced && pot != null ? cuts(pool.members(), pot) : null;
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < pool.members().size(); i++) {
            PoolMember m = pool.members().get(i);
            members.add(new Member(m.personId(), m.name(), m.share().setScale(2).toPlainString(), m.retired(),
                    cuts == null ? "" : cuts.get(i)));
        }
        return new PoolResponse(pool.id(), pool.name(), members, total.toPlainString(), balanced);
    }

    /** Each member's cut of the pot, in the members' order; the remainder goes to the largest share, earliest on a tie. */
    private static List<String> cuts(List<PoolMember> members, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            BigDecimal cut = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(HUNDRED).setScale(2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || m.share().compareTo(members.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        return cuts.stream().map(BigDecimal::toPlainString).toList();
    }
}

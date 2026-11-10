package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A pool, as the screen reads it: its members in the order added, the total of their shares (a retired
 * member's counting as 0.00) and whether that total is exactly 100.00. Each member of a balanced pool
 * has a cut of the sheet's pot: the pot times their share divided by 100, rounded half to even to two
 * decimals (a retired member's 0.00), with the remainder, the pot minus the cuts added up, going to the
 * member with the largest share, the earliest added on a tie, so the cuts total exactly the pot. An
 * unbalanced pool, or no pot, has no cuts.
 */
public record PoolResponse(String name, List<PoolMemberResponse> members, String totalShare, boolean balanced) {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public static PoolResponse from(Pool pool, List<PoolMember> members, BigDecimal pot) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (PoolMember m : members) if (!m.retired()) total = total.add(m.share());
        boolean balanced = total.compareTo(HUNDRED) == 0;
        List<BigDecimal> cuts = balanced && pot != null ? cuts(members, pot) : null;
        List<PoolMemberResponse> shown = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            shown.add(PoolMemberResponse.from(members.get(i), cuts == null ? null : cuts.get(i)));
        }
        return new PoolResponse(pool.name(), shown, total.toPlainString(), balanced);
    }

    private static List<BigDecimal> cuts(List<PoolMember> members, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            BigDecimal cut = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(HUNDRED).setScale(2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || m.share().compareTo(members.get(largest).share()) > 0)) largest = i;
        }
        if (largest >= 0) cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        return cuts;
    }
}

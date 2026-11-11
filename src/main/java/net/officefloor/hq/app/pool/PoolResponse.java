package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A pool, as the screen reads it: its members in the order added, the total of their shares (a retired
 * member's counting as 0.00) and whether that total is exactly 100.00. Each member of a balanced pool
 * has a cut of the sheet's pot: the pot times their share divided by the shares of the members with a cut
 * added up, rounded half to even to two decimals (a retired member's, and a member held on the sheet's
 * today, 0.00), with the remainder, the pot minus the cuts added up, going to the member with a cut with
 * the largest share, the earliest added on a tie, so the cuts total exactly the pot (each is 0.00 when no
 * member has a cut). A hold changes neither the total nor whether the pool is balanced. An
 * unbalanced pool, or no pot, has no cuts. A member whose dates do not include the sheet's today is
 * left out entirely: not in the total, not in the balance, and no cut (not even 0.00). A pool that is a member of another has no pot of its own: its
 * pot is its cut in the pool above (none when that pool has no cuts).
 */
public record PoolResponse(String name, List<PoolMemberResponse> members, String totalShare, boolean balanced) {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public static PoolResponse from(Pool pool, List<PoolMember> members, BigDecimal pot) {
        BigDecimal total = total(members);
        boolean balanced = total.compareTo(HUNDRED) == 0;
        List<BigDecimal> cuts = cutsOf(members, pot);
        List<PoolMemberResponse> shown = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            shown.add(PoolMemberResponse.from(members.get(i), cuts == null ? null : cuts.get(i)));
        }
        return new PoolResponse(pool.name(), shown, total.toPlainString(), balanced);
    }

    /** The active members' shares added up, a retired member's counting as 0.00. */
    static BigDecimal total(List<PoolMember> members) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        for (PoolMember m : members) if (m.active() && !m.retired()) total = total.add(m.share());
        return total;
    }

    /** Each member's cut of the pot, in the members' order; null when the pool is unbalanced or there is no pot. */
    static List<BigDecimal> cutsOf(List<PoolMember> members, BigDecimal pot) {
        return pot != null && total(members).compareTo(HUNDRED) == 0 ? cuts(members, pot) : null;
    }

    /** Whether the member has a cut: active, neither retired nor held on the sheet's today. */
    private static boolean hasCut(PoolMember m) {
        return m.active() && !m.retired() && !m.held();
    }

    private static List<BigDecimal> cuts(List<PoolMember> members, BigDecimal pot) {
        BigDecimal shares = BigDecimal.ZERO;
        for (PoolMember m : members) if (hasCut(m)) shares = shares.add(m.share());
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            if (!m.active()) {
                cuts.add(null);
                continue;
            }
            BigDecimal cut = !hasCut(m) || shares.signum() == 0 ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(shares, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (hasCut(m) && (largest < 0 || m.share().compareTo(members.get(largest).share()) > 0)) largest = i;
        }
        if (largest >= 0 && shares.signum() > 0) cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        return cuts;
    }
}

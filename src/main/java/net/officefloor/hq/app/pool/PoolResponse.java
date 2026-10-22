package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/** A pool as the screen shows it: its members in the order they were added, the total of their shares (a member
 *  retired by the date counts 0.00) and whether it is balanced (the total is exactly 100.00). */
public record PoolResponse(long id, String name, List<Member> members, String totalShare, boolean balanced) {

    /** A member: the person, their share, whether they are retired by the date, and their cut of the sheet's pot
     *  ('' when the pool is unbalanced or there is no pot). */
    public record Member(long personId, String name, String share, boolean retired, String cut) {
    }

    private static final BigDecimal FULL = new BigDecimal("100.00");

    /** The pool with its members; retiredFrom gives each person's retirement date (absent: not retired) and on is
     *  the date it is judged by (null: none retired). When the pool is balanced and there is a pot (null: none),
     *  each member's cut is the pot times their share divided by 100, rounded half to even to two decimals (a
     *  retired member cuts 0.00), and the pot less the cuts goes to the member with the largest share (the earliest
     *  added on a tie), so the cuts total exactly the pot. */
    public static PoolResponse from(Pool pool, List<PoolMember> members, Map<Long, LocalDate> retiredFrom,
            LocalDate on, BigDecimal pot) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean[] retired = new boolean[members.size()];
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            LocalDate from = retiredFrom.get(m.personId());
            retired[i] = on != null && from != null && !from.isAfter(on);
            if (!retired[i]) {
                total = total.add(m.share());
            }
        }
        boolean balanced = total.compareTo(FULL) == 0;
        BigDecimal[] cuts = balanced && pot != null ? cuts(members, retired, pot) : null;
        List<Member> shown = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            shown.add(new Member(m.personId(), m.name(), m.share().toPlainString(), retired[i],
                    cuts != null ? cuts[i].toPlainString() : ""));
        }
        return new PoolResponse(pool.id(), pool.name(), shown, total.toPlainString(), balanced);
    }

    public static PoolResponse from(Pool pool, List<PoolMember> members, LocalDate on) {
        return from(pool, members, Map.of(), on, null);
    }

    private static BigDecimal[] cuts(List<PoolMember> members, boolean[] retired, BigDecimal pot) {
        BigDecimal[] cuts = new BigDecimal[members.size()];
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            BigDecimal share = members.get(i).share();
            cuts[i] = retired[i] ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(FULL, 2, RoundingMode.HALF_EVEN);
            sum = sum.add(cuts[i]);
            if (!retired[i] && (largest < 0 || share.compareTo(members.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts[largest] = cuts[largest].add(pot.subtract(sum));
        }
        return cuts;
    }
}

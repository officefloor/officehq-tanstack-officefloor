package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A pool as the screen shows it: its members in the order they were added, the total of their shares (a member
 *  retired by the date counts 0.00) and whether it is balanced (the total is exactly 100.00). */
public record PoolResponse(long id, String name, List<Member> members, String totalShare, boolean balanced) {

    /** A member: the person (or, for a member pool, null and the pool's id in poolId), their name, their share,
     *  whether they are retired by the date, and their cut of the pool's pot ('' when the pool is unbalanced or there
     *  is no pot). */
    public record Member(Long personId, String name, String share, boolean retired, String cut, Long poolId) {
    }

    /** This member pool's cut, or null when it is not a member here or has no cut. */
    public BigDecimal cutOf(long memberPoolId) {
        return members.stream().filter(m -> m.poolId() != null && m.poolId() == memberPoolId && !m.cut().isEmpty())
                .map(m -> new BigDecimal(m.cut())).findFirst().orElse(null);
    }

    private static final BigDecimal FULL = new BigDecimal("100.00");

    /** The pool with its members; retiredFrom gives each person's retirement date (absent: not retired), held the
     *  people with a hold in force on the date, and on is the date it is judged by (null: none retired). When the
     *  pool is balanced and there is a pot (null: none), a retired or held member cuts 0.00 and each other member's
     *  cut is the pot (the sheet's, or for a member pool its cut in the pool above) times their share divided by the
     *  shares of the members with a cut, added up, rounded half to even to two decimals, and the pot less the cuts
     *  goes to the member with a cut with the largest share (the earliest added on a tie), so the cuts total exactly
     *  the pot. A hold leaves the total share and balance alone. */
    public static PoolResponse from(Pool pool, List<PoolMember> members, Map<Long, LocalDate> retiredFrom,
            Set<Long> held, LocalDate on, BigDecimal pot) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean[] retired = new boolean[members.size()];
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            LocalDate from = m.isPool() ? null : retiredFrom.get(m.personId());
            retired[i] = on != null && from != null && !from.isAfter(on);
            if (!retired[i]) {
                total = total.add(m.share());
            }
        }
        boolean balanced = total.compareTo(FULL) == 0;
        boolean[] noCut = new boolean[members.size()];
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            noCut[i] = retired[i] || (!m.isPool() && held.contains(m.personId()));
        }
        BigDecimal[] cuts = balanced && pot != null ? cuts(members, noCut, pot) : null;
        List<Member> shown = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            shown.add(new Member(m.isPool() ? null : m.personId(), m.name(), m.share().toPlainString(), retired[i],
                    cuts != null ? cuts[i].toPlainString() : "", m.memberPoolId()));
        }
        return new PoolResponse(pool.id(), pool.name(), shown, total.toPlainString(), balanced);
    }

    public static PoolResponse from(Pool pool, List<PoolMember> members, LocalDate on) {
        return from(pool, members, Map.of(), Set.of(), on, null);
    }

    private static BigDecimal[] cuts(List<PoolMember> members, boolean[] noCut, BigDecimal pot) {
        BigDecimal[] cuts = new BigDecimal[members.size()];
        BigDecimal shares = BigDecimal.ZERO;
        for (int i = 0; i < members.size(); i++) {
            if (!noCut[i]) {
                shares = shares.add(members.get(i).share());
            }
        }
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            BigDecimal share = members.get(i).share();
            cuts[i] = noCut[i] ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(share).divide(shares, 2, RoundingMode.HALF_EVEN);
            sum = sum.add(cuts[i]);
            if (!noCut[i] && (largest < 0 || share.compareTo(members.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts[largest] = cuts[largest].add(pot.subtract(sum));
        }
        return cuts;
    }
}

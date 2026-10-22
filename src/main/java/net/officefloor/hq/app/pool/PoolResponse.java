package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** A pool as the screen shows it: its members in the order they were added, the total of their shares (a member
 *  retired by the date counts their share as given) and whether it is balanced (the total is exactly 100.00). */
public record PoolResponse(long id, String name, List<Member> members, String totalShare, boolean balanced) {

    /** A member: the person (or, for a member pool, null and the pool's id in poolId), their name, their share,
     *  whether they are retired by the date, and their cut of the pool's pot ('' when the pool is unbalanced or there
     *  is no pot, or when the membership's dates leave them out), the membership's from- and to-dates ('' when open)
     *  and whether those dates leave them out on the date. */
    public record Member(Long personId, String name, String share, boolean retired, String cut, Long poolId,
            String from, String to, boolean outside) {
    }

    /** This member pool's cut, or null when it is not a member here or has no cut. */
    public BigDecimal cutOf(long memberPoolId) {
        return members.stream().filter(m -> m.poolId() != null && m.poolId() == memberPoolId && !m.cut().isEmpty())
                .map(m -> new BigDecimal(m.cut())).findFirst().orElse(null);
    }

    private static final BigDecimal FULL = new BigDecimal("100.00");

    /** The pool with its members; retiredFrom gives each person's retirement date (absent: not retired), held the
     *  people with a hold in force on the date, and on is the date it is judged by (null: none retired). When the
     *  pool is balanced and there is a pot (null: none), a retired or held member cuts 0.00 (their share spread over the others) and each other member's
     *  cut is the pot (the sheet's, or for a member pool its cut in the pool above) times their share divided by the
     *  shares of the members with a cut, added up, rounded half to even to two decimals, and the pot less the cuts
     *  goes to the member with a cut with the largest share (the earliest added on a tie), so the cuts total exactly
     *  the pot; when every member is retired there are no cuts ('' shown). A hold or a retirement leaves the total
     *  share and balance alone. A member whose membership dates do not include the
     *  date is left out entirely: not in the total share, not in the balance, and no cut ('' shown). */
    public static PoolResponse from(Pool pool, List<PoolMember> members, Map<Long, LocalDate> retiredFrom,
            Set<Long> held, LocalDate on, BigDecimal pot) {
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean[] retired = new boolean[members.size()];
        boolean[] outside = new boolean[members.size()];
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            outside[i] = !m.covers(on);
            if (outside[i]) {
                continue;
            }
            LocalDate from = m.isPool() ? null : retiredFrom.get(m.personId());
            retired[i] = on != null && from != null && !from.isAfter(on);
            total = total.add(m.share());
        }
        boolean balanced = total.compareTo(FULL) == 0;
        boolean[] noCut = new boolean[members.size()];
        boolean allRetired = true;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            noCut[i] = outside[i] || retired[i] || (!m.isPool() && held.contains(m.personId()));
            if (!outside[i] && !retired[i]) {
                allRetired = false;
            }
        }
        BigDecimal[] cuts = balanced && pot != null && !allRetired ? cuts(members, noCut, pot) : null;
        List<Member> shown = new ArrayList<>();
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            shown.add(new Member(m.isPool() ? null : m.personId(), m.name(), m.share().toPlainString(), retired[i],
                    cuts != null && !outside[i] ? cuts[i].toPlainString() : "", m.memberPoolId(), date(m.from()),
                    date(m.to()), outside[i]));
        }
        return new PoolResponse(pool.id(), pool.name(), shown, total.toPlainString(), balanced);
    }

    public static PoolResponse from(Pool pool, List<PoolMember> members, LocalDate on) {
        return from(pool, members, Map.of(), Set.of(), on, null);
    }

    private static String date(LocalDate d) {
        return d == null ? "" : d.toString();
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

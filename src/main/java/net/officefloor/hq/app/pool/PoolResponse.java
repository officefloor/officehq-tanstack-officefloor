package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * A pool as the screen reads it: its members' shares with two decimals and their membership dates (null: no start /
 * no end), the total share (a retired member's share, whatever their retirement date, counts as given; a member whose
 * dates do not include the sheet's today is left out entirely: not in the total, no cut, "" for its cut) and whether that total is exactly 100.00. In a balanced pool, with a
 * pot, each member with a cut (neither retired nor held on the sheet's today) gets the pot times their share divided by
 * the shares of the members with a cut added up, rounded half to even to two decimals (a retired or held member's is
 * 0.00, so a hold spreads its share over the others); the remainder (the pot less the cuts added up) goes to the member
 * with a cut with the largest share, the earliest added on a tie, so the cuts total exactly the pot (when no member has
 * a cut, each is 0.00; when every member in the pool is retired, there are no cuts). Otherwise a member's cut is "" (none). A member is a
 * person ({@code personId}) or another pool ({@code poolId}); the pool's pot is the sheet's when it is a member of no
 * pool, else its cut in the pool above (null: none).
 */
public record PoolResponse(long id, String name, List<Member> members, String totalShare, boolean balanced) {

    public record Member(Long personId, Long poolId, String name, String share, boolean retired, String cut,
            String from, String to) {
    }

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public static PoolResponse from(Pool pool, BigDecimal pot) {
        BigDecimal total = total(pool);
        boolean balanced = total.compareTo(HUNDRED) == 0;
        List<BigDecimal> cuts = cuts(pool, pot);
        List<Member> members = new ArrayList<>();
        for (int i = 0; i < pool.members().size(); i++) {
            PoolMember m = pool.members().get(i);
            members.add(new Member(m.personId(), m.poolId(), m.name(), m.share().setScale(2).toPlainString(),
                    m.retired(), cuts == null || cuts.get(i) == null ? "" : cuts.get(i).toPlainString(),
                    m.from() == null ? null : m.from().toString(), m.to() == null ? null : m.to().toString()));
        }
        return new PoolResponse(pool.id(), pool.name(), members, total.toPlainString(), balanced);
    }

    /** The pool's total share: its members' shares added up, a retired member's counting as given, an out one's not. */
    static BigDecimal total(Pool pool) {
        return pool.members().stream().filter(m -> !m.out()).map(PoolMember::share)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    /**
     * Each member's cut of the pot, in the members' order; the remainder goes to the largest share, earliest on a tie.
     * An out member's (its dates leave out the sheet's today) is null: none. Null when the pool is unbalanced or there
     * is no pot, or when every member in the pool on the sheet's today is retired.
     */
    static List<BigDecimal> cuts(Pool pool, BigDecimal pot) {
        if (pot == null || total(pool).compareTo(HUNDRED) != 0) {
            return null;
        }
        List<PoolMember> members = pool.members();
        if (members.stream().filter(m -> !m.out()).allMatch(PoolMember::retired)) {
            return null;
        }
        BigDecimal base = members.stream().filter(PoolMember::cut).map(PoolMember::share)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO;
        int largest = -1;
        for (int i = 0; i < members.size(); i++) {
            PoolMember m = members.get(i);
            if (m.out()) {
                cuts.add(null);
                continue;
            }
            BigDecimal cut = !m.cut() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(base, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (m.cut() && (largest < 0 || m.share().compareTo(members.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        return cuts;
    }
}

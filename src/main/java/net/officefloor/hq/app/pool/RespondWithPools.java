package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools with their members, total share and whether they are balanced, and each member's cut
 *  of the pool's pot: the sheet's pot for a pool that is a member of none, else its cut in the pool above. */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PoolStore store, SettingStore settings,
            ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate on = History.date(asOf);
        BigDecimal sheetPot = (on == null ? settings : settings.asOf(on)).pot(sheet);
        Map<Long, List<PoolStore.Member>> members = store.members(sheet, on);
        List<PoolStore.Pool> pools = store.list(sheet, on);
        // each member pool to the pool it is a member of, and its share there
        Map<Long, Long> parents = new HashMap<>();
        members.forEach((poolId, ms) -> ms.stream().filter(m -> m.memberPoolId() != null)
                .forEach(m -> parents.put(m.memberPoolId(), poolId)));
        Map<Long, List<BigDecimal>> cuts = new HashMap<>();
        for (PoolStore.Pool p : pools) {
            cutsOf(p.id(), members, parents, sheetPot, cuts, 0);
        }
        response.send(pools.stream().map(p -> {
            List<PoolStore.Member> ms = members.getOrDefault(p.id(), List.of());
            List<BigDecimal> cs = cuts.get(p.id());
            List<PoolResponse.MemberResponse> shown = new ArrayList<>();
            for (int i = 0; i < ms.size(); i++) {
                PoolStore.Member m = ms.get(i);
                shown.add(new PoolResponse.MemberResponse(m.personId(), m.memberPoolId(), m.name(),
                        m.share().setScale(2).toPlainString(), m.retired(),
                        cs == null ? null : cs.get(i).toPlainString()));
            }
            BigDecimal total = total(ms);
            return new PoolResponse(p.id(), p.name(), shown, total.toPlainString(), total.compareTo(HUNDRED) == 0);
        }).toList());
    }

    /** The total of the members' shares, a retired member's counting as 0.00. */
    private static BigDecimal total(List<PoolStore.Member> ms) {
        return ms.stream().filter(m -> !m.retired()).map(PoolStore.Member::share)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    /** The pool's members' cuts of its pot, null when it is not balanced or has no pot: the sheet's pot for a
     *  pool that is a member of none, else its cut in the pool above. Kept in {@code cuts} as worked out. */
    private static List<BigDecimal> cutsOf(long poolId, Map<Long, List<PoolStore.Member>> members,
            Map<Long, Long> parents, BigDecimal sheetPot, Map<Long, List<BigDecimal>> cuts, int depth) {
        if (cuts.containsKey(poolId) || depth > members.size() + 1) {
            return cuts.get(poolId);
        }
        BigDecimal pot = sheetPot;
        Long parent = parents.get(poolId);
        if (parent != null) {
            List<BigDecimal> above = cutsOf(parent, members, parents, sheetPot, cuts, depth + 1);
            pot = null;
            List<PoolStore.Member> siblings = members.getOrDefault(parent, List.of());
            for (int i = 0; above != null && i < siblings.size(); i++) {
                if (Long.valueOf(poolId).equals(siblings.get(i).memberPoolId())) {
                    pot = above.get(i);
                }
            }
        }
        List<PoolStore.Member> ms = members.getOrDefault(poolId, List.of());
        List<BigDecimal> found = total(ms).compareTo(HUNDRED) == 0 && pot != null ? cuts(ms, pot) : null;
        cuts.put(poolId, found);
        return found;
    }

    /** Each member's cut, in the members' order: the pot times their share over 100, rounded half to even to
     *  two decimals (a retired member's is 0.00); the remainder goes to the member with the largest share, the
     *  earliest added on a tie, so the cuts total exactly the pot. */
    private static List<BigDecimal> cuts(List<PoolStore.Member> ms, BigDecimal pot) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < ms.size(); i++) {
            PoolStore.Member m = ms.get(i);
            BigDecimal cut = m.retired() ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(HUNDRED).setScale(2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!m.retired() && (largest < 0 || m.share().compareTo(ms.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)).setScale(2));
        }
        return cuts;
    }
}

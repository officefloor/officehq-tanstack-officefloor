package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.setting.SettingStore;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools with their members, total share and whether they are balanced, and each member's cut
 *  of the pool's pot: the sheet's pot for a pool that is a member of none, else its cut in the pool above. A
 *  person with a hold in force on the sheet's today (the as-of date when one is given) gets no cut. A member
 *  whose dates do not include that day is left out entirely: not in the total share, not in the balance, no cut. */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PoolStore store, SettingStore settings,
            PersonStore people, SheetStore sheets, ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate on = History.date(asOf);
        BigDecimal sheetPot = (on == null ? settings : settings.asOf(on)).pot(sheet);
        Map<Long, List<PoolStore.Member>> members = store.members(sheet, on);
        List<PoolStore.Pool> pools = store.list(sheet, on);
        LocalDate today = on == null ? sheets.today(sheet) : on;
        Set<Long> held = (on == null ? people.list(sheet) : people.listAsOf(sheet, on)).stream()
                .filter(p -> p.getHolds().stream().anyMatch(h -> h.covers(today))).map(Person::getId)
                .collect(Collectors.toSet());
        // each member pool to the pool it is a member of, and its share there
        Map<Long, Long> parents = new HashMap<>();
        members.forEach((poolId, ms) -> ms.stream().filter(m -> m.memberPoolId() != null)
                .forEach(m -> parents.put(m.memberPoolId(), poolId)));
        Map<Long, List<BigDecimal>> cuts = new HashMap<>();
        for (PoolStore.Pool p : pools) {
            cutsOf(p.id(), members, parents, held, today, sheetPot, cuts, 0);
        }
        response.send(pools.stream().map(p -> {
            List<PoolStore.Member> ms = members.getOrDefault(p.id(), List.of());
            List<BigDecimal> cs = cuts.get(p.id());
            List<PoolResponse.MemberResponse> shown = new ArrayList<>();
            for (int i = 0; i < ms.size(); i++) {
                PoolStore.Member m = ms.get(i);
                shown.add(new PoolResponse.MemberResponse(m.personId(), m.memberPoolId(), m.name(),
                        m.share().setScale(2).toPlainString(), m.retired(),
                        cs == null || cs.get(i) == null ? null : cs.get(i).toPlainString(),
                        m.from() == null ? null : m.from().toString(), m.to() == null ? null : m.to().toString()));
            }
            BigDecimal total = total(ms, today);
            return new PoolResponse(p.id(), p.name(), shown, total.toPlainString(), total.compareTo(HUNDRED) == 0);
        }).toList());
    }

    /** The total of the members' shares, a retired member's counting as given, one whose dates do not include
     *  today as 0.00. */
    private static BigDecimal total(List<PoolStore.Member> ms, LocalDate today) {
        return ms.stream().filter(m -> m.covers(today)).map(PoolStore.Member::share)
                .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2);
    }

    /** The pool's members' cuts of its pot, null when it is not balanced or has no pot: the sheet's pot for a
     *  pool that is a member of none, else its cut in the pool above. Kept in {@code cuts} as worked out. */
    private static List<BigDecimal> cutsOf(long poolId, Map<Long, List<PoolStore.Member>> members,
            Map<Long, Long> parents, Set<Long> held, LocalDate today, BigDecimal sheetPot,
            Map<Long, List<BigDecimal>> cuts, int depth) {
        if (cuts.containsKey(poolId) || depth > members.size() + 1) {
            return cuts.get(poolId);
        }
        BigDecimal pot = sheetPot;
        Long parent = parents.get(poolId);
        if (parent != null) {
            List<BigDecimal> above = cutsOf(parent, members, parents, held, today, sheetPot, cuts, depth + 1);
            pot = null;
            List<PoolStore.Member> siblings = members.getOrDefault(parent, List.of());
            for (int i = 0; above != null && i < siblings.size(); i++) {
                if (Long.valueOf(poolId).equals(siblings.get(i).memberPoolId())) {
                    pot = above.get(i);
                }
            }
        }
        List<PoolStore.Member> ms = members.getOrDefault(poolId, List.of());
        List<BigDecimal> found = total(ms, today).compareTo(HUNDRED) == 0 && pot != null ? cuts(ms, held, today, pot) : null;
        cuts.put(poolId, found);
        return found;
    }

    /** Each member's cut, in the members' order: the pot times their share over the shares of the members with a
     *  cut, rounded half to even to two decimals (a retired or held member's is 0.00, their share spread over the
     *  others, and each is 0.00 when none has a cut); null for every member when each whose dates include today is
     *  retired. The remainder goes to the member with a cut with the largest share, the earliest added on a
     *  tie, so the cuts total exactly the pot. A member whose dates do not include today has no cut (null). */
    private static List<BigDecimal> cuts(List<PoolStore.Member> ms, Set<Long> held, LocalDate today,
            BigDecimal pot) {
        List<PoolStore.Member> current = ms.stream().filter(m -> m.covers(today)).toList();
        if (!current.isEmpty() && current.stream().allMatch(PoolStore.Member::retired)) {
            List<BigDecimal> none = new ArrayList<>();
            ms.forEach(m -> none.add(null));
            return none;
        }
        BigDecimal shares = ms.stream().filter(m -> m.covers(today) && hasCut(m, held)).map(PoolStore.Member::share)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < ms.size(); i++) {
            PoolStore.Member m = ms.get(i);
            if (!m.covers(today)) {
                cuts.add(null);
                continue;
            }
            boolean cut = hasCut(m, held) && shares.signum() > 0;
            BigDecimal c = cut ? pot.multiply(m.share()).divide(shares, 2, RoundingMode.HALF_EVEN)
                    : BigDecimal.ZERO.setScale(2);
            cuts.add(c);
            sum = sum.add(c);
            if (cut && (largest < 0 || m.share().compareTo(ms.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)).setScale(2));
        }
        return cuts;
    }

    /** Whether the member gets a cut: not retired, and not a person with a hold in force. */
    private static boolean hasCut(PoolStore.Member m, Set<Long> held) {
        return !m.retired() && (m.personId() == null || !held.contains(m.personId()));
    }
}

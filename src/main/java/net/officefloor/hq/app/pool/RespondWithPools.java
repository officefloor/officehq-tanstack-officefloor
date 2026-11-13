package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import net.officefloor.hq.app.crew.CrewStore;
import net.officefloor.hq.app.history.History;
import net.officefloor.hq.app.period.PeriodResponse;
import net.officefloor.hq.app.period.PeriodStore;
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
 *  whose dates do not include that day is left out entirely: not in the total share, not in the balance, no cut.
 *  Each crew is a pool too, after the stored pools in the order the crews were added: under the crew's name (and
 *  the negated crew id), its lead then its members each with an equal share of 100.00 (split as patrons' shares
 *  are, the remainder on the lead), and no dates. Each member also has its cut as it stood at the end of each
 *  period whose to-date is on or before that day (the pools, members, pot and holds as they were then, with the
 *  to-date as the sheet's today), null when it had none; for a closed or soft-closed period (whatever its to-date),
 *  the cut fixed when it was closed instead (see {@link PoolCuts}). */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PoolStore store, SettingStore settings,
            PersonStore people, SheetStore sheets, CrewStore crews, PeriodStore periods, PoolCuts fixedCuts,
            ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate on = History.date(asOf);
        LocalDate today = on == null ? sheets.today(sheet) : on;
        Cuts now = cuts(sheet, on, today, store, settings, people, crews);
        // each closed or soft-closed period with the cuts fixed when it was closed; each other period ended by
        // today with the cuts as they stood at its end (that day as the sheet's today)
        Map<String, java.util.function.BiFunction<Long, PoolStore.Member, String>> then = new LinkedHashMap<>();
        for (PeriodResponse p : on == null ? periods.list(sheet) : periods.listAsOf(sheet, on)) {
            LocalDate end = LocalDate.parse(p.to());
            if (p.closed() || p.soft()) {
                List<PoolCuts.Fixed> fixed = fixedCuts.fixed(sheet, p.name());
                then.put(p.name(), (poolId, m) -> PoolCuts.cutOf(fixed, poolId, m));
            } else if (!end.isAfter(today)) {
                Cuts c = cuts(sheet, end, end, store, settings, people, crews);
                then.put(p.name(), c::cutOf);
            }
        }
        response.send(now.pools.stream().map(p -> {
            List<PoolStore.Member> ms = now.members.getOrDefault(p.id(), List.of());
            List<BigDecimal> cs = now.cuts.get(p.id());
            List<PoolResponse.MemberResponse> shown = new ArrayList<>();
            for (int i = 0; i < ms.size(); i++) {
                PoolStore.Member m = ms.get(i);
                List<PoolResponse.PeriodCut> periodCuts = new ArrayList<>();
                then.forEach((name, c) -> periodCuts.add(new PoolResponse.PeriodCut(name, c.apply(p.id(), m))));
                shown.add(new PoolResponse.MemberResponse(m.personId(), m.memberPoolId(), m.name(),
                        m.share().setScale(2).toPlainString(), m.retired(),
                        cs == null || cs.get(i) == null ? null : cs.get(i).toPlainString(),
                        m.from() == null ? null : m.from().toString(), m.to() == null ? null : m.to().toString(),
                        periodCuts));
            }
            BigDecimal total = total(ms, today);
            return new PoolResponse(p.id(), p.name(), shown, total.toPlainString(), total.compareTo(HUNDRED) == 0,
                    now.crewPools.contains(p.id()));
        }).toList());
    }

    /** The sheet's pools, their members and the members' cuts, as they stood at the end of {@code on} (now when
     *  null), with {@code today} as the sheet's today. */
    record Cuts(List<PoolStore.Pool> pools, Map<Long, List<PoolStore.Member>> members,
            Map<Long, List<BigDecimal>> cuts, Set<Long> crewPools) {

        /** The member's cut in the pool (matched by the person or member pool), null when it had none. */
        String cutOf(long poolId, PoolStore.Member m) {
            List<PoolStore.Member> ms = members.getOrDefault(poolId, List.of());
            List<BigDecimal> cs = cuts.get(poolId);
            for (int i = 0; cs != null && i < ms.size(); i++) {
                PoolStore.Member o = ms.get(i);
                if (Objects.equals(o.personId(), m.personId()) && Objects.equals(o.memberPoolId(), m.memberPoolId())) {
                    return cs.get(i) == null ? null : cs.get(i).toPlainString();
                }
            }
            return null;
        }
    }

    static Cuts cuts(String sheet, LocalDate on, LocalDate today, PoolStore store, SettingStore settings,
            PersonStore people, CrewStore crews) {
        BigDecimal sheetPot = (on == null ? settings : settings.asOf(on)).pot(sheet);
        Map<Long, List<PoolStore.Member>> members = store.members(sheet, on);
        List<PoolStore.Pool> pools = new ArrayList<>(store.list(sheet, on));
        List<Person> everyone = on == null ? people.list(sheet) : people.listAsOf(sheet, on);
        Set<Long> crewPools = addCrews(sheet, on, crews, everyone, pools, members);
        Set<Long> held = everyone.stream()
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
        return new Cuts(pools, members, cuts, crewPools);
    }

    /** Adds each crew as a pool (id the negated crew id) with its lead and members still on the sheet at equal
     *  shares; the ids of the pools added. */
    private static Set<Long> addCrews(String sheet, LocalDate on, CrewStore crews, List<Person> everyone,
            List<PoolStore.Pool> pools, Map<Long, List<PoolStore.Member>> members) {
        Map<Long, Person> byId = new HashMap<>();
        everyone.forEach(p -> byId.put(p.getId(), p));
        Map<Long, List<CrewStore.Member>> crewMembers = crews.members(sheet, on);
        Set<Long> added = new java.util.HashSet<>();
        for (CrewStore.Crew c : crews.list(sheet, on)) {
            long id = -c.id();
            List<Long> ids = new ArrayList<>();
            ids.add(c.leadId());
            crewMembers.getOrDefault(c.id(), List.of()).forEach(m -> ids.add(m.personId()));
            List<Person> in = ids.stream().map(byId::get).filter(java.util.Objects::nonNull).toList();
            List<BigDecimal> shares = equalShares(in.size());
            List<PoolStore.Member> ms = new ArrayList<>();
            for (int i = 0; i < in.size(); i++) {
                Person p = in.get(i);
                ms.add(new PoolStore.Member(id, p.getId(), null, p.getName(), shares.get(i), p.isRetired(), null,
                        null));
            }
            pools.add(new PoolStore.Pool(id, c.name()));
            members.put(id, ms);
            added.add(id);
        }
        return added;
    }

    /** 100.00 split equally over {@code count}, as patrons' shares are: each the even split rounded half to even
     *  to two decimals, the remainder on the first. */
    private static List<BigDecimal> equalShares(int count) {
        List<BigDecimal> shares = new ArrayList<>();
        if (count == 0) {
            return shares;
        }
        BigDecimal each = HUNDRED.divide(BigDecimal.valueOf(count), 2, RoundingMode.HALF_EVEN);
        for (int i = 0; i < count; i++) {
            shares.add(each);
        }
        shares.set(0, HUNDRED.subtract(each.multiply(BigDecimal.valueOf(count - 1))).setScale(2));
        return shares;
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
     *  retired. The remainder goes to the member with the smallest non-zero cut, the latest added on a tie
     *  (a member with no cut gets none; when no cut is non-zero, to the last member with a cut and a share), so
     *  the cuts total exactly the pot. A member whose dates do not include today has no cut (null). */
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
        int smallest = -1;
        int last = -1;
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
            if (cut && m.share().signum() > 0) {
                last = i;
            }
            if (cut && c.signum() > 0 && (smallest < 0 || c.compareTo(cuts.get(smallest)) <= 0)) {
                smallest = i;
            }
        }
        int to = smallest >= 0 ? smallest : last;
        if (to >= 0) {
            cuts.set(to, cuts.get(to).add(pot.subtract(sum)).setScale(2));
        }
        return cuts;
    }

    /** Whether the member gets a cut: not retired, and not a person with a hold in force. */
    private static boolean hasCut(PoolStore.Member m, Set<Long> held) {
        return !m.retired() && (m.personId() == null || !held.contains(m.personId()));
    }
}

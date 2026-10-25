package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.crew.Crew;
import net.officefloor.hq.app.crew.CrewMember;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.sheet.InvalidTodayException;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools, in the order they were added, each with its members in the order they were added. A member
 *  with a retirement date set (whatever the date) counts its share toward the pool's total, as any member does. A pool that is a member of none has the sheet's pot; a pool that is a member of another has
 *  its cut in that pool as its pot (none when that pool has no cuts). Each member of a balanced pool, when it has a
 *  pot, cuts the pot times their share divided by the shares of the members with a cut added up, rounded half to even
 *  to two decimals; a retired member, and a person with a hold covering the sheet's today, has no cut (0.00), so
 *  their share is spread over the others in proportion to theirs (the pool's total share is unchanged). A pool whose
 *  every member is retired shows no cuts. The remainder, the pot minus the cuts added up, goes to the member with the
 *  smallest non-zero cut, the latest added on a tie (a member with no cut gets none; a pool with no non-zero cut gives
 *  it to the last member with a share), so the cuts total exactly the pot. A member whose dates do not include the
 *  sheet's today is shown with its share but left out entirely: not in the total share, nor the balance, and no cut.
 *  As of a date (asOf), the sheet's pot and today are as they stood at the end of it, and a person not then on the
 *  sheet is left out (as are the pools, members and crews not then made). Each crew is also a pool, listed after them
 *  (see crewPool). Each member also has, for each period whose to-date is on or before the sheet's today (by
 *  from-date), its cut as the sheet stood at the end of that period, that day its today: "" when it had none. */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@Val Sheet current, @RequestParam(name = "asOf", required = false) String asOfParam, AsOf asOf,
            ObjectResponse<List<PoolResponse>> response)
            throws InvalidTodayException {
        LocalDate date = AsOf.dateOf(asOfParam);
        Sheet sheet = asOf.sheet(current, date);
        List<PoolResponse> shown = poolsAsOf(current, date, asOf);
        // Each period ended by the sheet's today: every member's cut as the sheet stood at the end of it.
        List<Period> ended = asOf.periods(current.getId(), date).stream()
                .filter(p -> sheet.getToday() != null && p.getToDate() != null && !p.getToDate().isAfter(sheet.getToday()))
                .toList();
        Map<String, Map<String, String>> cutsThen = new HashMap<>();
        for (Period period : ended) {
            Map<String, String> then = new HashMap<>();
            for (PoolResponse pool : poolsAsOf(current, period.getToDate(), asOf)) {
                pool.members().forEach(m -> then.put(memberKey(pool, m), m.cut()));
            }
            cutsThen.put(period.getName(), then);
        }
        List<PoolResponse> out = new ArrayList<>();
        for (PoolResponse pool : shown) {
            List<PoolResponse.Member> members = new ArrayList<>();
            for (PoolResponse.Member m : pool.members()) {
                List<PoolResponse.PeriodCut> periodCuts = new ArrayList<>();
                for (Period period : ended) {
                    periodCuts.add(new PoolResponse.PeriodCut(period.getName(),
                            cutsThen.get(period.getName()).getOrDefault(memberKey(pool, m), "")));
                }
                members.add(m.withPeriodCuts(periodCuts));
            }
            out.add(new PoolResponse(pool.id(), pool.name(), pool.totalShare(), pool.balanced(), pool.crew(), members));
        }
        response.send(out);
    }

    /** The member of the pool it is: the pool's id with the person, or the member pool. */
    private static String memberKey(PoolResponse pool, PoolResponse.Member m) {
        return pool.id() + (m.poolId() != null ? ":pool:" + m.poolId() : ":person:" + m.personId());
    }

    /** The sheet's pools, then its crews, as they stood at the end of the date (now, when none). */
    private static List<PoolResponse> poolsAsOf(Sheet current, LocalDate date, AsOf asOf) {
        Sheet sheet = asOf.sheet(current, date);
        Map<Long, Person> byId = asOf.people(current.getId(), date).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        List<Pool> sheetPools = asOf.pools(current.getId(), date);
        Map<Long, Pool> poolsById = sheetPools.stream().collect(Collectors.toMap(Pool::getId, Function.identity()));
        Map<Long, List<PoolMember>> allMembers = asOf.poolMembers(current.getId(), date);
        Map<Long, List<PoolMember>> membersOf = new HashMap<>();
        Set<Long> inAPool = new HashSet<>();
        for (Pool pool : sheetPools) {
            List<PoolMember> list = allMembers.getOrDefault(pool.getId(), List.of());
            membersOf.put(pool.getId(), list);
            list.forEach(m -> { if (m.getMemberPoolId() != null) inAPool.add(m.getMemberPoolId()); });
        }
        // Pots flow down from the pools that are members of none.
        Map<Long, PoolResponse> shownById = new HashMap<>();
        Deque<Map.Entry<Long, BigDecimal>> todo = new ArrayDeque<>();
        for (Pool pool : sheetPools) {
            if (!inAPool.contains(pool.getId())) todo.add(new SimpleEntry<>(pool.getId(), sheet.getPot()));
        }
        while (!todo.isEmpty()) {
            Map.Entry<Long, BigDecimal> next = todo.poll();
            Pool pool = poolsById.get(next.getKey());
            if (pool == null || shownById.containsKey(pool.getId())) continue;
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            List<PoolMember> kept = new ArrayList<>();
            List<String> names = new ArrayList<>();
            List<BigDecimal> counted = new ArrayList<>();
            List<Boolean> held = new ArrayList<>();
            List<Boolean> dated = new ArrayList<>();
            List<Boolean> retired = new ArrayList<>();
            for (PoolMember member : membersOf.get(pool.getId())) {
                BigDecimal share;
                if (member.getMemberPoolId() != null) {
                    Pool sub = poolsById.get(member.getMemberPoolId());
                    if (sub == null) continue;
                    names.add(sub.getName());
                    share = member.getShare();
                    held.add(false);
                    retired.add(false);
                } else {
                    Person person = byId.get(member.getPersonId());
                    if (person == null) continue;
                    names.add(person.getName());
                    share = member.getShare();
                    boolean isRetired = person.getRetiredFrom() != null;
                    held.add(isRetired || (sheet.getToday() != null && person.heldOn(sheet.getToday())));
                    retired.add(isRetired);
                }
                boolean in = member.includes(sheet.getToday());
                dated.add(in);
                if (in) {
                    total = total.add(share);
                    counted.add(share);
                }
                kept.add(member);
            }
            boolean balanced = total.compareTo(HUNDRED) == 0;
            List<Boolean> heldIn = new ArrayList<>();
            boolean allRetired = true;
            for (int i = 0; i < kept.size(); i++) {
                if (!dated.get(i)) continue;
                heldIn.add(held.get(i));
                if (!retired.get(i)) allRetired = false;
            }
            List<String> countedCuts = cuts(balanced && !allRetired ? next.getValue() : null, counted, heldIn);
            List<String> cuts = new ArrayList<>();
            for (int i = 0, j = 0; i < kept.size(); i++) cuts.add(dated.get(i) ? countedCuts.get(j++) : "");
            List<PoolResponse.Member> shown = new ArrayList<>();
            for (int i = 0; i < kept.size(); i++) {
                PoolMember m = kept.get(i);
                shown.add(new PoolResponse.Member(m.getPersonId(), m.getMemberPoolId(), names.get(i),
                        m.getShare().setScale(2).toPlainString(), cuts.get(i)));
                if (m.getMemberPoolId() != null) {
                    todo.add(new SimpleEntry<>(m.getMemberPoolId(), cuts.get(i).isEmpty() ? null : new BigDecimal(cuts.get(i))));
                }
            }
            shownById.put(pool.getId(), new PoolResponse(pool.getId(), pool.getName(), total.toPlainString(), balanced, false, shown));
        }
        List<PoolResponse> out = new ArrayList<>();
        for (Pool pool : sheetPools) {
            if (shownById.containsKey(pool.getId())) out.add(shownById.get(pool.getId()));
        }
        // Each crew is also a pool, after the sheet's pools: its lead then its members at equal shares, with the
        // sheet's pot, cut as any pool that is a member of none.
        Map<Long, List<CrewMember>> crewMembersOf = asOf.crewMembers(current.getId(), date);
        for (Crew crew : asOf.crews(current.getId(), date)) {
            out.add(crewPool(crew, crewMembersOf.getOrDefault(crew.getId(), List.of()), byId, sheet));
        }
        return out;
    }

    /** The crew as a pool (its id the crew's, negated): the lead and the members, in the order they were added, each
     *  at an equal share of 100.00, rounded half to even to two decimals, the remainder on the lead. A person not on
     *  the sheet (as of the date) is left out, keeping their share; a crew with no lead (transferred away) shares among
     *  its members alone. */
    private static PoolResponse crewPool(Crew crew, List<CrewMember> crewMembers, Map<Long, Person> byId, Sheet sheet) {
        List<Long> ids = new ArrayList<>();
        if (crew.getLeadId() != null) ids.add(crew.getLeadId());
        crewMembers.forEach(m -> ids.add(m.getPersonId()));
        if (ids.isEmpty()) return new PoolResponse(-crew.getId(), crew.getName(), "0.00", false, true, List.of());
        BigDecimal each = HUNDRED.divide(BigDecimal.valueOf(ids.size()), 2, RoundingMode.HALF_EVEN);
        List<BigDecimal> shares = new ArrayList<>();
        ids.forEach(id -> shares.add(each));
        shares.set(0, each.add(HUNDRED.subtract(each.multiply(BigDecimal.valueOf(ids.size())))));
        List<Person> kept = new ArrayList<>();
        List<BigDecimal> counted = new ArrayList<>();
        List<Boolean> held = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO.setScale(2);
        boolean allRetired = true;
        for (int i = 0; i < ids.size(); i++) {
            Person person = byId.get(ids.get(i));
            if (person == null) continue;
            boolean isRetired = person.getRetiredFrom() != null;
            kept.add(person);
            counted.add(shares.get(i));
            held.add(isRetired || (sheet.getToday() != null && person.heldOn(sheet.getToday())));
            if (!isRetired) allRetired = false;
            total = total.add(shares.get(i));
        }
        boolean balanced = total.compareTo(HUNDRED) == 0;
        List<String> cuts = cuts(balanced && !allRetired ? sheet.getPot() : null, counted, held);
        List<PoolResponse.Member> shown = new ArrayList<>();
        for (int i = 0; i < kept.size(); i++) {
            shown.add(new PoolResponse.Member(kept.get(i).getId(), null, kept.get(i).getName(),
                    counted.get(i).setScale(2).toPlainString(), cuts.get(i)));
        }
        return new PoolResponse(-crew.getId(), crew.getName(), total.toPlainString(), balanced, true, shown);
    }

    /** Each member's cut of the pot by their counted share out of the counted shares of those not held, in order: ""
     *  for all when there is no pot (or the pool is not balanced); 0.00 for a held member. The remainder goes to the
     *  smallest non-zero cut, the latest on a tie; with no non-zero cut, to the last not held with a share. */
    private static List<String> cuts(BigDecimal pot, List<BigDecimal> shares, List<Boolean> held) {
        List<String> out = new ArrayList<>();
        if (pot == null) {
            shares.forEach(s -> out.add(""));
            return out;
        }
        BigDecimal withCut = BigDecimal.ZERO.setScale(2);
        for (int i = 0; i < shares.size(); i++) {
            if (!held.get(i)) withCut = withCut.add(shares.get(i));
        }
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int smallest = -1;
        int lastShared = -1;
        for (int i = 0; i < shares.size(); i++) {
            if (held.get(i) || withCut.signum() == 0) {
                cuts.add(BigDecimal.ZERO.setScale(2));
                continue;
            }
            BigDecimal cut = pot.multiply(shares.get(i)).divide(withCut, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (cut.signum() != 0 && (smallest < 0 || cut.compareTo(cuts.get(smallest)) <= 0)) smallest = i;
            if (shares.get(i).signum() != 0) lastShared = i;
        }
        int to = smallest >= 0 ? smallest : lastShared;
        if (to >= 0) cuts.set(to, cuts.get(to).add(pot.subtract(sum)));
        cuts.forEach(c -> out.add(c.setScale(2).toPlainString()));
        return out;
    }
}

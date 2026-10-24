package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
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
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's pools, in the order they were added, each with its members in the order they were added. A member
 *  with a retirement date set (whatever the date) counts as 0.00 toward the pool's total; a member that is another
 *  pool counts its share. A pool that is a member of none has the sheet's pot; a pool that is a member of another has
 *  its cut in that pool as its pot (none when that pool has no cuts). Each member of a balanced pool, when it has a
 *  pot, cuts the pot times their share divided by 100, rounded half to even to two decimals (a retired member 0.00);
 *  the remainder, the pot minus the cuts added up, goes to the member with the largest share (a retired member's
 *  counting as 0.00), the earliest added on a tie, so the cuts total exactly the pot. */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@Val Sheet sheet, PoolRepository pools, PoolMemberRepository members, PersonRepository people,
            ObjectResponse<List<PoolResponse>> response) {
        Map<Long, Person> byId = people.findListed(sheet.getId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        List<Pool> sheetPools = pools.findBySheetIdOrderByIdAsc(sheet.getId());
        Map<Long, Pool> poolsById = sheetPools.stream().collect(Collectors.toMap(Pool::getId, Function.identity()));
        Map<Long, List<PoolMember>> membersOf = new HashMap<>();
        Set<Long> inAPool = new HashSet<>();
        for (Pool pool : sheetPools) {
            List<PoolMember> list = members.findByPoolIdOrderByIdAsc(pool.getId());
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
            for (PoolMember member : membersOf.get(pool.getId())) {
                BigDecimal share;
                if (member.getMemberPoolId() != null) {
                    Pool sub = poolsById.get(member.getMemberPoolId());
                    if (sub == null) continue;
                    names.add(sub.getName());
                    share = member.getShare();
                } else {
                    Person person = byId.get(member.getPersonId());
                    if (person == null) continue;
                    names.add(person.getName());
                    share = person.getRetiredFrom() == null ? member.getShare() : BigDecimal.ZERO.setScale(2);
                }
                total = total.add(share);
                kept.add(member);
                counted.add(share);
            }
            boolean balanced = total.compareTo(HUNDRED) == 0;
            List<String> cuts = cuts(balanced ? next.getValue() : null, counted);
            List<PoolResponse.Member> shown = new ArrayList<>();
            for (int i = 0; i < kept.size(); i++) {
                PoolMember m = kept.get(i);
                shown.add(new PoolResponse.Member(m.getPersonId(), m.getMemberPoolId(), names.get(i),
                        m.getShare().setScale(2).toPlainString(), cuts.get(i)));
                if (m.getMemberPoolId() != null) {
                    todo.add(new SimpleEntry<>(m.getMemberPoolId(), cuts.get(i).isEmpty() ? null : new BigDecimal(cuts.get(i))));
                }
            }
            shownById.put(pool.getId(), new PoolResponse(pool.getId(), pool.getName(), total.toPlainString(), balanced, shown));
        }
        List<PoolResponse> out = new ArrayList<>();
        for (Pool pool : sheetPools) {
            if (shownById.containsKey(pool.getId())) out.add(shownById.get(pool.getId()));
        }
        response.send(out);
    }

    /** Each member's cut of the pot by their counted share, in order: "" for all when there is no pot (or the pool
     *  is not balanced). The remainder goes to the largest share, the earliest on a tie. */
    private static List<String> cuts(BigDecimal pot, List<BigDecimal> shares) {
        List<String> out = new ArrayList<>();
        if (pot == null) {
            shares.forEach(s -> out.add(""));
            return out;
        }
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < shares.size(); i++) {
            BigDecimal cut = pot.multiply(shares.get(i)).divide(HUNDRED, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (largest < 0 || shares.get(i).compareTo(shares.get(largest)) > 0) largest = i;
        }
        if (largest >= 0) cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        cuts.forEach(c -> out.add(c.setScale(2).toPlainString()));
        return out;
    }
}

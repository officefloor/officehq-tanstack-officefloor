package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's pools, in the order they were added, each with its members in the order they were added. A member
 *  with a retirement date set (whatever the date) counts as 0.00 toward the pool's total. Each member of a balanced
 *  pool, when the sheet has a pot, cuts the pot times their share divided by 100, rounded half to even to two decimals
 *  (a retired member 0.00); the remainder, the pot minus the cuts added up, goes to the member with the largest share
 *  (a retired member's counting as 0.00), the earliest added on a tie, so the cuts total exactly the pot. */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@Val Sheet sheet, PoolRepository pools, PoolMemberRepository members, PersonRepository people,
            ObjectResponse<List<PoolResponse>> response) {
        Map<Long, Person> byId = people.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        List<PoolResponse> out = new ArrayList<>();
        for (Pool pool : pools.findBySheetIdOrderByIdAsc(sheet.getId())) {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            List<Person> persons = new ArrayList<>();
            List<BigDecimal> shares = new ArrayList<>();
            List<BigDecimal> counted = new ArrayList<>();
            for (PoolMember member : members.findByPoolIdOrderByIdAsc(pool.getId())) {
                Person person = byId.get(member.getPersonId());
                if (person == null) continue;
                BigDecimal share = person.getRetiredFrom() == null ? member.getShare() : BigDecimal.ZERO.setScale(2);
                total = total.add(share);
                persons.add(person);
                shares.add(member.getShare());
                counted.add(share);
            }
            boolean balanced = total.compareTo(HUNDRED) == 0;
            List<String> cuts = cuts(balanced ? sheet.getPot() : null, counted);
            List<PoolResponse.Member> shown = new ArrayList<>();
            for (int i = 0; i < persons.size(); i++) {
                shown.add(new PoolResponse.Member(persons.get(i).getId(), persons.get(i).getName(),
                        shares.get(i).setScale(2).toPlainString(), cuts.get(i)));
            }
            out.add(new PoolResponse(pool.getId(), pool.getName(), total.toPlainString(), balanced, shown));
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

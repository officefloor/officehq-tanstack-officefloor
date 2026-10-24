package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
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
 *  with a retirement date set (whatever the date) counts as 0.00 toward the pool's total. */
public class RespondWithPools {
    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    public void respond(@Val Sheet sheet, PoolRepository pools, PoolMemberRepository members, PersonRepository people,
            ObjectResponse<List<PoolResponse>> response) {
        Map<Long, Person> byId = people.findBySheetIdOrderByIdAsc(sheet.getId()).stream()
                .collect(Collectors.toMap(Person::getId, Function.identity()));
        List<PoolResponse> out = new ArrayList<>();
        for (Pool pool : pools.findBySheetIdOrderByIdAsc(sheet.getId())) {
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            List<PoolResponse.Member> shown = new ArrayList<>();
            for (PoolMember member : members.findByPoolIdOrderByIdAsc(pool.getId())) {
                Person person = byId.get(member.getPersonId());
                if (person == null) continue;
                if (person.getRetiredFrom() == null) total = total.add(member.getShare());
                shown.add(new PoolResponse.Member(person.getId(), person.getName(),
                        member.getShare().setScale(2).toPlainString()));
            }
            out.add(new PoolResponse(pool.getId(), pool.getName(), total.toPlainString(),
                    total.compareTo(HUNDRED) == 0, shown));
        }
        response.send(out);
    }
}

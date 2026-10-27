package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPools {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    // A member with a retirement date set, whatever the date, counts 0.00 toward the pool's total.
    public void respond(@PathVariable("sheetId") String sheetId, Pools pools, People people,
            ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        Map<Long, Person> byId = people.list(sheet).stream().collect(Collectors.toMap(Person::id, Function.identity()));
        List<Pools.Membership> members = pools.members(sheet);
        List<PoolResponse> out = new ArrayList<>();
        for (String name : pools.names(sheet)) {
            List<PoolResponse.Member> shown = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (Pools.Membership m : members) {
                Person person = byId.get(m.personId());
                if (!m.pool().equals(name) || person == null) {
                    continue;
                }
                boolean retired = person.retiredFrom() != null;
                shown.add(new PoolResponse.Member(person.id(), person.name(), m.share().toPlainString(), retired));
                if (!retired) {
                    total = total.add(m.share());
                }
            }
            out.add(new PoolResponse(name, shown, total.toPlainString(), total.compareTo(HUNDRED) == 0));
        }
        response.send(out);
    }
}

package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPools {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    // A member with a retirement date set, whatever the date, counts 0.00 toward the pool's total.
    // Each member of a balanced pool cuts the pot times their share / 100, half to even to two decimals (a retired member
    // 0.00); the remainder goes to the member with the largest share, the earliest added on a tie, so the cuts total the pot.
    public void respond(@PathVariable("sheetId") String sheetId, Pools pools, People people, Settings settings,
            ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        Map<Long, Person> byId = people.list(sheet).stream().collect(Collectors.toMap(Person::id, Function.identity()));
        List<Pools.Membership> members = pools.members(sheet);
        BigDecimal pot = settings.pot(sheet);
        List<PoolResponse> out = new ArrayList<>();
        for (String name : pools.names(sheet)) {
            List<Pools.Membership> in = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (Pools.Membership m : members) {
                Person person = byId.get(m.personId());
                if (!m.pool().equals(name) || person == null) {
                    continue;
                }
                in.add(m);
                if (person.retiredFrom() == null) {
                    total = total.add(m.share());
                }
            }
            boolean balanced = total.compareTo(HUNDRED) == 0;
            List<BigDecimal> cuts = balanced && pot != null ? cuts(pot, in, byId) : null;
            List<PoolResponse.Member> shown = new ArrayList<>();
            for (int i = 0; i < in.size(); i++) {
                Pools.Membership m = in.get(i);
                Person person = byId.get(m.personId());
                shown.add(new PoolResponse.Member(person.id(), person.name(), m.share().toPlainString(),
                        person.retiredFrom() != null, cuts == null ? "" : cuts.get(i).toPlainString()));
            }
            out.add(new PoolResponse(name, shown, total.toPlainString(), balanced));
        }
        response.send(out);
    }

    private static List<BigDecimal> cuts(BigDecimal pot, List<Pools.Membership> in, Map<Long, Person> byId) {
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < in.size(); i++) {
            Pools.Membership m = in.get(i);
            boolean retired = byId.get(m.personId()).retiredFrom() != null;
            BigDecimal cut = retired ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(HUNDRED, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!retired && (largest < 0 || m.share().compareTo(in.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        return cuts;
    }
}

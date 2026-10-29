package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPools {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    // A member with a retirement date set, whatever the date, counts 0.00 toward the pool's total.
    // Each member of a balanced pool cuts the pool's pot times their share / 100, half to even to two decimals (a retired
    // member 0.00); the remainder goes to the member with the largest share, the earliest added on a tie, so the cuts
    // total the pot. A member held on the sheet's today (a retired member too) has no cut: the pot is spread over the
    // members with a cut, each the pot times their share / the shares of the members with a cut added up (all 0.00 when
    // none has a cut); the total share and balance are unchanged by a hold. A pool that is a member of none has the sheet's pot; a pool that is a member of another has its cut
    // there as its pot (none when the pool above has no cuts). A member whose dates do not include the sheet's today is
    // left out entirely: listed with its share, but not in the total share or the balance, and with no cut ("").
    public void respond(@PathVariable("sheetId") String sheetId, Pools pools, People people, Settings settings, Sheets sheets,
            ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        Map<Long, Person> byId = people.list(sheet).stream().collect(Collectors.toMap(Person::id, Function.identity()));
        List<Pools.Membership> members = pools.members(sheet);
        Map<String, String> parents = pools.parents(sheet);
        BigDecimal sheetPot = settings.pot(sheet);
        List<String> names = pools.names(sheet);
        LocalDate today = sheets.today(sheet);

        Map<String, List<Pools.Membership>> inByPool = new HashMap<>();
        Map<String, BigDecimal> totals = new HashMap<>();
        for (String name : names) {
            List<Pools.Membership> in = new ArrayList<>();
            BigDecimal total = BigDecimal.ZERO.setScale(2);
            for (Pools.Membership m : members) {
                if (!m.pool().equals(name)) {
                    continue;
                }
                if (!m.on(today)) {
                    in.add(m);
                    continue;
                }
                if (m.memberPool() == null) {
                    Person person = byId.get(m.personId());
                    if (person == null) {
                        continue;
                    }
                    if (person.retiredFrom() == null) {
                        total = total.add(m.share());
                    }
                } else {
                    total = total.add(m.share());
                }
                in.add(m);
            }
            inByPool.put(name, in);
            totals.put(name, total);
        }

        // Each pool's cuts, figured from the top of its chain down (null when it has none).
        Map<String, List<BigDecimal>> cutsByPool = new HashMap<>();
        for (String name : names) {
            cutsOf(name, sheetPot, parents, inByPool, totals, byId, today, cutsByPool);
        }

        List<PoolResponse> out = new ArrayList<>();
        for (String name : names) {
            List<Pools.Membership> in = inByPool.get(name);
            List<BigDecimal> cuts = cutsByPool.get(name);
            List<PoolResponse.Member> shown = new ArrayList<>();
            for (int i = 0; i < in.size(); i++) {
                Pools.Membership m = in.get(i);
                String cut = cuts == null || !m.on(today) ? "" : cuts.get(i).toPlainString();
                if (m.memberPool() != null) {
                    shown.add(new PoolResponse.Member(0, m.memberPool(), m.share().toPlainString(), false, cut,
                            m.memberPool()));
                } else {
                    Person person = byId.get(m.personId());
                    shown.add(new PoolResponse.Member(person.id(), person.name(), m.share().toPlainString(),
                            person.retiredFrom() != null, cut, null));
                }
            }
            BigDecimal total = totals.get(name);
            out.add(new PoolResponse(name, shown, total.toPlainString(), total.compareTo(HUNDRED) == 0));
        }
        response.send(out);
    }

    private static List<BigDecimal> cutsOf(String name, BigDecimal sheetPot, Map<String, String> parents,
            Map<String, List<Pools.Membership>> inByPool, Map<String, BigDecimal> totals, Map<Long, Person> byId,
            LocalDate today, Map<String, List<BigDecimal>> cutsByPool) {
        if (cutsByPool.containsKey(name)) {
            return cutsByPool.get(name);
        }
        BigDecimal pot = sheetPot;
        String parent = parents.get(name);
        if (parent != null) {
            pot = null;
            List<BigDecimal> above = cutsOf(parent, sheetPot, parents, inByPool, totals, byId, today, cutsByPool);
            if (above != null) {
                List<Pools.Membership> in = inByPool.get(parent);
                for (int i = 0; i < in.size(); i++) {
                    if (name.equals(in.get(i).memberPool()) && in.get(i).on(today)) {
                        pot = above.get(i);
                    }
                }
            }
        }
        boolean balanced = totals.get(name).compareTo(HUNDRED) == 0;
        List<BigDecimal> cuts = balanced && pot != null ? cuts(pot, inByPool.get(name), byId, today) : null;
        cutsByPool.put(name, cuts);
        return cuts;
    }

    /**
     * Whether the member gets no cut: one whose dates do not include the sheet's today, or a person who is retired or held
     * on it.
     */
    private static boolean noCut(Pools.Membership m, Map<Long, Person> byId, LocalDate today) {
        if (!m.on(today)) {
            return true;
        }
        if (m.memberPool() != null) {
            return false;
        }
        Person person = byId.get(m.personId());
        return person.retiredFrom() != null || person.heldOn(today);
    }

    private static List<BigDecimal> cuts(BigDecimal pot, List<Pools.Membership> in, Map<Long, Person> byId,
            LocalDate today) {
        BigDecimal shares = BigDecimal.ZERO.setScale(2);
        for (Pools.Membership m : in) {
            if (!noCut(m, byId, today)) {
                shares = shares.add(m.share());
            }
        }
        boolean anyCut = shares.signum() > 0;
        List<BigDecimal> cuts = new ArrayList<>();
        BigDecimal sum = BigDecimal.ZERO.setScale(2);
        int largest = -1;
        for (int i = 0; i < in.size(); i++) {
            Pools.Membership m = in.get(i);
            boolean none = !anyCut || noCut(m, byId, today);
            BigDecimal cut = none ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(shares, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (!none && (largest < 0 || m.share().compareTo(in.get(largest).share()) > 0)) {
                largest = i;
            }
        }
        if (largest >= 0) {
            cuts.set(largest, cuts.get(largest).add(pot.subtract(sum)));
        }
        return cuts;
    }
}

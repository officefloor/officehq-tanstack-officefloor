package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import net.officefloor.hq.app.crew.CrewResponse;
import net.officefloor.hq.app.crew.Crews;
import net.officefloor.hq.app.history.AsOf;
import net.officefloor.hq.app.period.Period;
import net.officefloor.hq.app.period.Periods;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithPools {

    private static final BigDecimal HUNDRED = new BigDecimal("100.00");

    // Each crew is also a pool, listed after the stored pools under the crew's name: its lead then its members, at
    // equal shares of 100.00 (each rounded half to even, the remainder on the lead), with no dates.
    // A member with a retirement date set, whatever the date, counts their share as given toward the pool's total.
    // Each member of a balanced pool cuts the pool's pot times their share / 100, half to even to two decimals; the remainder goes to the member with the smallest non-zero cut, the latest added on a tie (a member with no cut gets
    // none; with no non-zero cut it goes to the last member with a share), so the cuts
    // total the pot. A member held on the sheet's today, or retired, has a cut of 0.00: the pot is spread over the
    // members with a cut, each the pot times their share / the shares of the members with a cut added up (all 0.00 when
    // none has a cut); the total share and balance are unchanged by a hold or a retirement. A pool whose every member
    // (whose dates include the sheet's today) is retired has no cuts. A pool that is a member of none has the sheet's pot; a pool that is a member of another has its cut
    // there as its pot (none when the pool above has no cuts). A member whose dates do not include the sheet's today is
    // left out entirely: listed with its share, but not in the total share or the balance, and with no cut ("").
    // For every period whose to-date is on or before the sheet's today, each member also shows its cut as it stood at the
    // end of that period: figured as above from the store as of the to-date (the pools, their members, the pot and the
    // holds as they were then, with that day as the sheet's today); "" when it had none, or was not a member then.
    public void respond(@PathVariable("sheetId") String sheetId, Pools pools, People people, Settings settings, Sheets sheets,
            Crews crews, Periods periods, ObjectResponse<List<PoolResponse>> response) {
        String sheet = sheetId.trim();
        LocalDate today = sheets.today(sheet);
        List<Period> ended = periods.list(sheet).stream().filter(p -> !p.to().isAfter(today)).toList();
        Map<String, Map<String, String>> cutsThen = new LinkedHashMap<>();
        for (Period period : ended) {
            Map<String, String> byMember = new HashMap<>();
            for (PoolResponse pool : AsOf.on(period.to(), () -> figure(sheet, pools, people, settings, sheets, crews,
                    Map.of(), List.of()))) {
                for (PoolResponse.Member m : pool.members()) {
                    byMember.put(key(pool.name(), m), m.cut());
                }
            }
            cutsThen.put(period.name(), byMember);
        }
        response.send(figure(sheet, pools, people, settings, sheets, crews, cutsThen,
                ended.stream().map(Period::name).toList()));
    }

    /** Names a member of a pool the same way at every date. */
    private static String key(String pool, PoolResponse.Member m) {
        return pool + "\n" + (m.pool() != null ? "pool:" + m.pool() : String.valueOf(m.personId()));
    }

    /** The sheet's pools as the store is read now; each member with its cut at the end of each named period. */
    private static List<PoolResponse> figure(String sheet, Pools pools, People people, Settings settings, Sheets sheets,
            Crews crews, Map<String, Map<String, String>> cutsThen, List<String> ended) {
        Map<Long, Person> byId = people.list(sheet).stream().collect(Collectors.toMap(Person::id, Function.identity()));
        List<Pools.Membership> members = new ArrayList<>(pools.members(sheet));
        Map<String, String> parents = pools.parents(sheet);
        BigDecimal sheetPot = settings.pot(sheet);
        List<String> names = new ArrayList<>(pools.names(sheet));
        for (CrewResponse crew : crews.list(sheet)) {
            names.add(crew.name());
            members.addAll(crewMembers(crew, byId));
        }
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
                    total = total.add(m.share());
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
                PoolResponse.Member member;
                if (m.memberPool() != null) {
                    member = new PoolResponse.Member(0, m.memberPool(), m.share().toPlainString(), false, cut,
                            m.memberPool(), List.of());
                } else {
                    Person person = byId.get(m.personId());
                    member = new PoolResponse.Member(person.id(), person.name(), m.share().toPlainString(),
                            person.retiredFrom() != null, cut, null, List.of());
                }
                List<PoolResponse.PeriodCut> then = new ArrayList<>();
                for (String period : ended) {
                    then.add(new PoolResponse.PeriodCut(period,
                            cutsThen.getOrDefault(period, Map.of()).getOrDefault(key(name, member), "")));
                }
                shown.add(member.withPeriodCuts(then));
            }
            BigDecimal total = totals.get(name);
            out.add(new PoolResponse(name, shown, total.toPlainString(), total.compareTo(HUNDRED) == 0));
        }
        return out;
    }

    /** The crew as a pool's members: its lead then its members (people of the sheet), at equal shares of 100.00. */
    private static List<Pools.Membership> crewMembers(CrewResponse crew, Map<Long, Person> byId) {
        List<Long> ids = new ArrayList<>();
        ids.add(crew.leadId());
        ids.addAll(crew.memberIds());
        ids.removeIf(id -> !byId.containsKey(id));
        List<Pools.Membership> out = new ArrayList<>();
        if (ids.isEmpty()) {
            return out;
        }
        BigDecimal each = HUNDRED.divide(BigDecimal.valueOf(ids.size()), 2, RoundingMode.HALF_EVEN);
        BigDecimal first = HUNDRED.subtract(each.multiply(BigDecimal.valueOf(ids.size() - 1)));
        for (int i = 0; i < ids.size(); i++) {
            out.add(new Pools.Membership(crew.name(), ids.get(i), i == 0 ? first : each, null, null, null));
        }
        return out;
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
        List<BigDecimal> cuts = balanced && pot != null && !allRetired(inByPool.get(name), byId, today)
                ? cuts(pot, inByPool.get(name), byId, today) : null;
        cutsByPool.put(name, cuts);
        return cuts;
    }

    /** Whether every member whose dates include the sheet's today is a retired person. */
    private static boolean allRetired(List<Pools.Membership> in, Map<Long, Person> byId, LocalDate today) {
        boolean any = false;
        for (Pools.Membership m : in) {
            if (!m.on(today)) {
                continue;
            }
            if (m.memberPool() != null || byId.get(m.personId()).retiredFrom() == null) {
                return false;
            }
            any = true;
        }
        return any;
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
        int smallest = -1;
        int last = -1;
        for (int i = 0; i < in.size(); i++) {
            Pools.Membership m = in.get(i);
            boolean none = !anyCut || noCut(m, byId, today);
            BigDecimal cut = none ? BigDecimal.ZERO.setScale(2)
                    : pot.multiply(m.share()).divide(shares, 2, RoundingMode.HALF_EVEN);
            cuts.add(cut);
            sum = sum.add(cut);
            if (none) {
                continue;
            }
            last = i;
            if (cut.signum() != 0 && (smallest < 0 || cut.compareTo(cuts.get(smallest)) <= 0)) {
                smallest = i;
            }
        }
        int to = smallest >= 0 ? smallest : last;
        if (to >= 0) {
            cuts.set(to, cuts.get(to).add(pot.subtract(sum)));
        }
        return cuts;
    }
}

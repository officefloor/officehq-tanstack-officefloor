package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.crew.Crew;
import net.officefloor.hq.app.crew.CrewRepository;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The sheet's pools and crews as RespondWithPools shows them, worked out for the screen and for fixing a closing
 *  period's cuts. */
public final class ShownPools {

    private ShownPools() {
    }

    /** The sheet's pools and crews as shown on the sheet's today (or as of the end of the day, when one is given). */
    public static List<PoolResponse> list(String id, LocalDate end, PoolRepository repository, CrewRepository crews,
            PersonRepository people, SheetRepository sheets, SettingRepository settings) {
        return list(id, end, null, repository, crews, people, sheets, settings);
    }

    /** As list, the sheet's current records shown on the preview date instead of its today (when one is given and no
     *  as-of date is). */
    public static List<PoolResponse> list(String id, LocalDate end, LocalDate preview, PoolRepository repository,
            CrewRepository crews, PersonRepository people, SheetRepository sheets, SettingRepository settings) {
        LocalDate on = end != null ? end : preview != null ? preview : sheets.today(id);
        List<Person> persons = end != null ? people.listAsOf(id, end) : people.list(id);
        Map<Long, LocalDate> retiredFrom = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        Set<Long> held = new HashSet<>();
        BigDecimal pot = end != null ? settings.potAsOf(id, end) : settings.pot(id);
        for (Person p : persons) {
            names.put(p.id(), p.name());
            if (p.retiredFrom() != null) {
                retiredFrom.put(p.id(), p.retiredFrom());
            }
            if (on != null && p.holds().covers(on)) {
                held.add(p.id());
            }
        }
        List<Pool> pools = repository.list(id, end);
        Map<Long, String> poolNames = new HashMap<>();
        pools.forEach(p -> poolNames.put(p.id(), p.name()));
        Map<Long, List<PoolMember>> members = new HashMap<>();
        Map<Long, Pool> parents = new HashMap<>();
        for (Pool pool : pools) {
            List<PoolMember> named = repository.members(pool, end).stream()
                    .filter(m -> m.isPool() ? poolNames.containsKey(m.memberPoolId())
                            : names.containsKey(m.personId()))
                    .map(m -> m.named(m.isPool() ? poolNames.get(m.memberPoolId()) : names.get(m.personId())))
                    .toList();
            members.put(pool.id(), named);
            named.stream().filter(PoolMember::isPool).forEach(m -> parents.put(m.memberPoolId(), pool));
        }
        Map<Long, PoolResponse> shown = new HashMap<>();
        List<PoolResponse> all = new ArrayList<>(pools.stream()
                .map(pool -> shown(pool, members, parents, retiredFrom, held, on, pot, shown)).toList());
        for (Crew crew : crews.list(id)) {
            List<Long> ids = new ArrayList<>();
            if (names.containsKey(crew.leadId())) {
                ids.add(crew.leadId());
            }
            crews.members(crew).stream().filter(names::containsKey).forEach(ids::add);
            List<BigDecimal> shares = PoolResponse.equalShares(ids.size());
            Pool pool = new Pool(-crew.id(), id, crew.name(), crew.by());
            List<PoolMember> crewMembers = new ArrayList<>();
            for (int i = 0; i < ids.size(); i++) {
                crewMembers.add(new PoolMember(pool.id(), id, pool.name(), ids.get(i), names.get(ids.get(i)),
                        shares.get(i), crew.by()));
            }
            all.add(PoolResponse.from(pool, crewMembers, retiredFrom, held, on, pot).ofCrew(crew.id()));
        }
        return all;
    }

    /** The pool as shown, its pot the sheet's or its cut in the pool above (worked out first). */
    private static PoolResponse shown(Pool pool, Map<Long, List<PoolMember>> members, Map<Long, Pool> parents,
            Map<Long, LocalDate> retiredFrom, Set<Long> held, LocalDate on, BigDecimal sheetPot, Map<Long, PoolResponse> shown) {
        PoolResponse done = shown.get(pool.id());
        if (done != null) {
            return done;
        }
        Pool above = parents.get(pool.id());
        BigDecimal pot = above == null ? sheetPot
                : shown(above, members, parents, retiredFrom, held, on, sheetPot, shown).cutOf(pool.id());
        PoolResponse r = PoolResponse.from(pool, members.get(pool.id()), retiredFrom, held, on, pot);
        shown.put(pool.id(), r);
        return r;
    }
}

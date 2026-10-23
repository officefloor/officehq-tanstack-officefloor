package net.officefloor.hq.app.pool;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools in the order they were added, with their members, total share and whether balanced; a member
 *  retired by the sheet's today (or the as-of date, when one is given) cuts 0.00, their share still counted toward the total
 *  and spread over the members with a cut (no cuts at all when every member is retired); each member of a balanced pool
 *  shows their cut of the pool's pot (when there is one). A pool that is a member of none has the sheet's pot (as it stood
 *  then, given an as-of date); a pool
 *  that is a member of another has its cut there as its pot (none when the pool above has no cuts). A person with a
 *  hold in force on that date cuts 0.00, their share spread over the members with a cut. A member whose membership
 *  dates do not include that date is left out: no share in the total and no cut. */
public class RespondWithPools {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PoolRepository repository,
            PersonRepository people, SheetRepository sheets, SettingRepository settings,
            ObjectResponse<List<PoolResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        LocalDate on = end != null ? end : sheets.today(id);
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
        response.send(pools.stream()
                .map(pool -> shown(pool, members, parents, retiredFrom, held, on, pot, shown)).toList());
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

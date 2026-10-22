package net.officefloor.hq.app.pool;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.sheet.AsOf;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

/** The sheet's pools in the order they were added, with their members, total share and whether balanced; a member
 *  retired by the sheet's today (or the as-of date, when one is given) counts 0.00 toward the total. */
public class RespondWithPools {
    public void respond(@PathVariable("sheetId") String sheetId,
            @RequestParam(name = "asOf", required = false) String asOf, PoolRepository repository,
            PersonRepository people, SheetRepository sheets, ObjectResponse<List<PoolResponse>> response) {
        String id = sheetId.trim();
        LocalDate end = AsOf.parse(asOf);
        LocalDate on = end != null ? end : sheets.today(id);
        List<Person> persons = end != null ? people.listAsOf(id, end) : people.list(id);
        Map<Long, LocalDate> retiredFrom = new HashMap<>();
        Map<Long, String> names = new HashMap<>();
        for (Person p : persons) {
            names.put(p.id(), p.name());
            if (p.retiredFrom() != null) {
                retiredFrom.put(p.id(), p.retiredFrom());
            }
        }
        response.send(repository.list(id, end).stream().map(pool -> PoolResponse.from(pool,
                repository.members(pool, end).stream().filter(m -> names.containsKey(m.personId()))
                        .map(m -> new PoolMember(m.poolId(), m.sheetId(), m.pool(), m.personId(),
                                names.get(m.personId()), m.share(), m.by()))
                        .toList(),
                retiredFrom, on)).toList());
    }
}

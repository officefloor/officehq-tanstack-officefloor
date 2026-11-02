package net.officefloor.hq.app.crew;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.pool.Pools;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/** Adds a crew (its name trimmed) led by a person; refused for a blank name, one already a crew or a pool, or an unknown lead. */
public class AddCrew {
    public void add(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody CrewRequest request, Crews crews, Pools pools, People people,
            Audit audit) throws InvalidCrewException {
        String name = request == null || request.name() == null ? "" : request.name().trim();
        if (name.isEmpty()) {
            throw new InvalidCrewException("A crew's name must not be blank");
        }
        if (crews.has(sheet, name)) {
            throw new InvalidCrewException(name + " is already a crew");
        }
        if (pools.hasAdded(sheet, name)) {
            throw new InvalidCrewException(name + " is a pool");
        }
        Person lead = request.lead() == null ? null : people.find(sheet, request.lead());
        if (lead == null) {
            throw new InvalidCrewException("Choose the crew's lead");
        }
        crews.add(sheet, name, lead.id());
        audit.record("CREW_ADDED sheet=" + sheet + " by=" + user + " name=" + name + " lead=" + lead.name());
    }
}

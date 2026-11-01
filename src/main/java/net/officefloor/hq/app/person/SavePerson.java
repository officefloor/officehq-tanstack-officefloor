package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

public class SavePerson {
    public void save(@PathVariable("sheet") String sheet, @RequestParam("user") String user, @Val Person person,
            @Val PersonRequest request, People people, Audit audit, Rollup rollup)
            throws DuplicatePersonNameException, StalePersonException {
        // Saving over a version other than the one the editor loaded is refused, and writes nothing.
        long loaded = request.version() == null ? person.version() : request.version();
        if (loaded != person.version()) {
            throw new StalePersonException(person.id());
        }
        Rollup.Snapshot before = rollup.take(sheet);
        Person saved = new Person(person.id(), request.name(), request.grade(), loaded);
        people.update(sheet, saved);
        audit.record("PERSON_EDITED sheet=" + sheet + " by=" + user + " name=" + saved.name() + " grade="
                + saved.grade());
        rollup.record(sheet, user, before);
    }
}

package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Put a person on hold, or take them off it again. While held, their open (OPEN or PENDING) claims count in no live
 *  total; their SETTLED claims still do. */
public class HoldPerson {
    public void hold(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            ServerHttpConnection connection, PersonRepository repository, Audit audit, Rollup rollup,
            Out<Person> person) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person found = repository.find(id, pid);
        if (found == null) {
            throw new BadRequestException("no such person");
        }
        Rollup.Snapshot before = rollup.take(id);
        boolean held = !found.held();
        repository.hold(id, pid, held);
        audit.record((held ? "PERSON_HELD" : "PERSON_RELEASED") + " sheet=" + id + " by=" + by + " name=" + found.name());
        rollup.record(id, by, before);
        person.set(repository.find(id, pid));
    }
}

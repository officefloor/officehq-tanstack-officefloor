package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Unretire a retired person on the sheet's today: they are no longer retired, and the retirement is kept as a
 *  past one, from the date they were retired from to that day. Refused for anyone not retired. */
public class UnretirePerson {
    public void unretire(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            ServerHttpConnection connection, PersonRepository repository, SheetRepository sheets, Audit audit,
            Rollup rollup, Out<Person> person) throws BadRequestException {
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
        if (!found.retired()) {
            throw new BadRequestException(found.name() + " is not retired");
        }
        LocalDate on = sheets.today(id);
        Rollup.Snapshot before = rollup.take(id);
        repository.unretire(id, pid, LocalDate.parse(found.retiredFrom()), on);
        audit.record("PERSON_UNRETIRED sheet=" + id + " by=" + by + " name=" + found.name() + " on=" + on);
        rollup.record(id, by, before);
        person.set(repository.find(id, pid));
    }
}

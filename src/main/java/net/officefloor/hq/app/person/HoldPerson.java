package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Put a person on hold from a from-date (blank: the sheet's today) to an optional to-date. A person may
 *  have several holds; each ends on its to-date. Their claims still count in every total; a hold acts
 *  only on pool cuts (a person held on the sheet's today gets no cut, their share spread over the others). */
public class HoldPerson {
    public void hold(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val HoldInput input, ServerHttpConnection connection, PersonRepository repository,
            SheetRepository sheets, Audit audit, Rollup rollup, Out<Person> person) throws BadRequestException {
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
        LocalDate from = input.from() != null ? input.from() : sheets.today(id);
        if (input.to() != null && input.to().isBefore(from)) {
            throw new BadRequestException("to must not be before from");
        }
        Rollup.Snapshot before = rollup.take(id);
        repository.hold(id, pid, from, input.to());
        audit.record("PERSON_HELD sheet=" + id + " by=" + by + " name=" + found.name() + " from=" + from);
        rollup.record(id, by, before);
        person.set(repository.find(id, pid));
    }
}

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

/** Retire a person from a date (blank: the sheet's today). They count as retired at once, and no claim of
 *  theirs may be dated after that date; from the day the date is reached (the sheet's today on or after it)
 *  their open (OPEN or PENDING) claims not bound by a statement count in no live total, as a hold's would. */
public class RetirePerson {
    public void retire(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val RetireInput input, ServerHttpConnection connection, PersonRepository repository,
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
        LocalDate from = input.date() != null ? input.date() : sheets.today(id);
        Rollup.Snapshot before = rollup.take(id);
        repository.retire(id, pid, from);
        audit.record("PERSON_RETIRED sheet=" + id + " by=" + by + " name=" + found.name() + " from=" + from);
        rollup.record(id, by, before);
        person.set(repository.find(id, pid));
    }
}

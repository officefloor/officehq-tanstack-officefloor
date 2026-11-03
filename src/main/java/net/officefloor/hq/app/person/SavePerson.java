package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.web.bind.annotation.PathVariable;

/** Save an edited person; refused (409) when someone else has saved them since the editor loaded its version.
 *  The form's grade is recorded from its from-date (blank: the sheet's today), keeping their other grades,
 *  except that a save leaving the grade in force on the sheet's today as it is, with the from-date blank,
 *  records no grade. PERSON_EDITED carries the grade in force on the sheet's today after the save, then
 *  the person's own terms when the save changed them (empty when it cleared them), then their contact when
 *  the save changed it (empty when it cleared it). */
public class SavePerson {
    public void save(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @Val PersonRequest request, ServerHttpConnection connection, PersonRepository repository,
            SheetRepository sheets, Audit audit, Rollup rollup, Out<Person> person) throws BadRequestException, ConflictException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        if (request.version() == null) {
            throw new BadRequestException("version is required");
        }
        if (!repository.exists(id, pid)) {
            throw new BadRequestException("no such person");
        }
        if (repository.nameTaken(id, request.name(), pid)) {
            throw new BadRequestException("name is already in use");
        }
        if (repository.aliasClashes(id, pid)) {
            throw new BadRequestException("an alias is already in use on another sheet of the tree");
        }
        Person current = repository.find(id, pid);
        // a stale editor is told so (and shown the current values) before its contact is judged against them
        if (current.version() != request.version()) {
            throw new ConflictException("someone else has saved this person; showing their current values");
        }
        String contact = request.contact().isEmpty() ? null : request.contact();
        if (contact != null && repository.contactTaken(id, contact, pid)) {
            throw new BadRequestException("contact is already in use");
        }
        boolean regrade = !request.gradeFrom().isEmpty() || !request.grade().equals(current.grade());
        LocalDate from = request.gradeFrom().isEmpty() ? sheets.today(id) : LocalDate.parse(request.gradeFrom());
        Integer terms = request.terms().isEmpty() ? null : Integer.valueOf(request.terms());
        boolean reterm = !java.util.Objects.equals(terms, current.terms());
        boolean recontact = !java.util.Objects.equals(contact, current.contact());
        long version = request.version();
        Rollup.Snapshot before = rollup.take(id);
        boolean updated;
        try {
            updated = repository.update(id, pid, version, request.name(), terms, contact);
        } catch (DuplicateKeyException e) {
            throw new BadRequestException("name is already in use");
        }
        if (!updated) {
            throw new ConflictException("someone else has saved this person; showing their current values");
        }
        if (regrade) {
            repository.grade(id, pid, request.grade(), from);
        }
        Person saved = repository.find(id, pid);
        audit.record("PERSON_EDITED sheet=" + id + " by=" + by + " name=" + saved.name()
                + " grade=" + saved.grade() + (reterm ? " terms=" + (terms == null ? "" : terms) : "")
                + (recontact ? " contact=" + (contact == null ? "" : contact) : ""));
        rollup.record(id, by, before);
        person.set(saved);
    }
}

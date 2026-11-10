package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Give a person the grade chosen on their row from the sheet's today, exactly as the person form would save
 *  it with their own name, terms and contact: leaving the grade in force on the sheet's today as it is records
 *  no grade. Writes PERSON_EDITED and its rollup lines, and moves the person's version on, so an editor
 *  opened before it is refused as stale. */
public class GradePerson {

    public void grade(@PathVariable("sheetId") String sheetId, @PathVariable("personId") String personId,
            @RequestBody GradeRequest request, ServerHttpConnection connection, PersonRepository repository,
            SheetRepository sheets,
            net.officefloor.hq.app.settings.SettingsRepository settings, Audit audit, Rollup rollup, Out<Person> person)
            throws BadRequestException, ConflictException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        long pid;
        try {
            pid = Long.parseLong(personId == null ? "" : personId.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("no such person");
        }
        Person current = repository.find(id, pid);
        if (current == null) {
            throw new BadRequestException("no such person");
        }
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        settings.requireGrade(id, grade);
        boolean regrade = !grade.equals(current.grade());
        if (regrade) {
            NewPerson.checkNotRetired(settings, id, grade);
        }
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.update(id, pid, current.version(), current.name(), current.terms())) {
            throw new ConflictException("someone else has saved this person; showing their current values");
        }
        if (regrade) {
            repository.grade(id, pid, grade, sheets.today(id));
        }
        Person saved = repository.find(id, pid);
        audit.record("PERSON_EDITED sheet=" + id + " by=" + by + " name=" + saved.name() + " grade=" + saved.grade());
        rollup.record(id, by, before);
        person.set(saved);
    }
}

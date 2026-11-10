package net.officefloor.hq.app.importing;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.CurrentUser;
import net.officefloor.hq.app.person.NewPerson;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.settings.SettingsRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;
import org.springframework.web.bind.annotation.PathVariable;

/** Add or update a person per imported line, in order. A line whose name is a person's name or alias on the
 *  sheet (ignoring case) updates that person as the person form would save them with their own name: the
 *  grade is recorded from the sheet's today (none when it is unchanged), and their terms and contact are set
 *  when the line gives them; written as PERSON_EDITED. Any other line adds a person as the person form does,
 *  written as PERSON_ADDED. A line with a blank grade takes the sheet's default grade (A when it has none, or when it is retired). A grade retired on the sheet is refused, unless the line leaves a person on it as they are. One change: every line is checked first, so a refused line (or two lines naming
 *  the same person) changes no one and writes no audit record. */
public class ImportPeople {
    public void importPeople(@PathVariable("sheetId") String sheetId, @Val ImportInput input,
            ServerHttpConnection connection, PersonRepository repository, SheetRepository sheets, SettingsRepository settings, Audit audit,
            Rollup rollup, Out<ImportResult> result) throws BadRequestException {
        String by = CurrentUser.require(connection);
        String id = sheetId == null ? "" : sheetId.trim();
        if (!repository.sheetExists(id)) {
            throw new BadRequestException("no such sheet");
        }
        String defaultGrade = settings.defaultGrade(id);
        List<Person> matches = new ArrayList<>();
        Set<Long> matched = new HashSet<>();
        int n = 0;
        for (PersonRequest person : input.people()) {
            n++;
            try {
                Person current = repository.findByName(id, person.name());
                if (current == null) {
                    NewPerson.checkFree(repository, id, person);
                } else {
                    if (!matched.add(current.id())) {
                        throw new BadRequestException(current.name() + " is given twice in the import");
                    }
                    if (!person.contact().isEmpty() && repository.contactTaken(id, person.contact(), current.id())) {
                        throw new BadRequestException("contact is already in use");
                    }
                }
                String grade = person.grade().isEmpty() ? defaultGrade : person.grade();
                if (current == null || !grade.equals(current.grade())) {
                    NewPerson.checkNotRetired(settings, id, grade);
                }
                matches.add(current);
            } catch (BadRequestException e) {
                throw new BadRequestException("line " + n + ": " + e.getMessage());
            }
        }
        LocalDate today = sheets.today(id);
        int added = 0;
        int updated = 0;
        for (int i = 0; i < input.people().size(); i++) {
            PersonRequest person = input.people().get(i);
            if (person.grade().isEmpty()) {
                person = person.withGrade(defaultGrade);
            }
            Person current = matches.get(i);
            if (current == null) {
                NewPerson.add(repository, audit, rollup, id, by, person);
                added++;
            } else {
                update(repository, audit, rollup, id, by, today, current, person);
                updated++;
            }
        }
        result.set(new ImportResult(added, updated));
    }

    private static void update(PersonRepository repository, Audit audit, Rollup rollup, String id, String by,
            LocalDate today, Person current, PersonRequest person) throws BadRequestException {
        Integer terms = person.terms().isEmpty() ? current.terms() : Integer.valueOf(person.terms());
        String contact = person.contact().isEmpty() ? current.contact() : person.contact();
        boolean regrade = !person.grade().equals(current.grade());
        boolean reterm = !java.util.Objects.equals(terms, current.terms());
        boolean recontact = !java.util.Objects.equals(contact, current.contact());
        Rollup.Snapshot before = rollup.take(id);
        if (!repository.update(id, current.id(), current.version(), current.name(), terms, contact)) {
            throw new BadRequestException("someone else has saved " + current.name() + "; import again");
        }
        if (regrade) {
            repository.grade(id, current.id(), person.grade(), today);
        }
        Person saved = repository.find(id, current.id());
        audit.record("PERSON_EDITED sheet=" + id + " by=" + by + " name=" + saved.name()
                + " grade=" + saved.grade() + (reterm ? " terms=" + (terms == null ? "" : terms) : "")
                + (recontact ? " contact=" + (contact == null ? "" : contact) : ""));
        rollup.record(id, by, before);
    }
}

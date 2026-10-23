package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.hq.app.setting.SettingRepository;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Check every imported line against the sheet first, as the person form would. A line whose name is a name or alias
 *  of a person on the sheet (compared as names are compared) updates that person, checked as the form's edit is: their
 *  name and aliases not on another sheet of its tree, the line's contact not another person's, and no two lines naming
 *  the same person. Any other line adds a person, checked as the form's add is: the name not on another sheet of its
 *  tree, the contact not another person's. Any failure refuses the whole import, naming the line, and nothing is
 *  written. A line with a blank grade takes the sheet's default grade. Otherwise each line is applied in order just as the form would: an update records the grade from the
 *  sheet's today and sets the terms and contact when the line gives them (blank: left as they are), writing
 *  PERSON_EDITED; an add writes PERSON_ADDED; each then how the sheet's figures moved over it. */
public class ImportPeople {
    public void importPeople(@PathVariable("sheetId") String sheetId, @Val ImportInput input,
            PersonRepository repository, SheetRepository sheets, SettingRepository settings, Rollups rollups, Audit audit,
            Out<ImportResponse> result) throws InvalidPersonException {
        String sheet = sheetId.trim();
        List<Person> matches = new ArrayList<>();
        Set<Long> updating = new HashSet<>();
        for (ImportLine line : input.lines()) {
            PersonRequest request = line.person();
            Person match = repository.findByName(sheet, request.name());
            try {
                if (match != null) {
                    if (!updating.add(match.id())) {
                        throw new DuplicatePersonNameException(match.name());
                    }
                    DuplicatePersonNameException.refuseInTree(sheet, match.name(), repository);
                    for (String alias : match.aliases()) {
                        DuplicatePersonNameException.refuseInTree(sheet, alias, repository);
                    }
                } else {
                    DuplicatePersonNameException.refuseInTree(sheet, request.name(), repository);
                }
                if (!request.contact().isEmpty() && repository.contactInUse(sheet, request.contact(),
                        match == null ? null : match.id())) {
                    throw new DuplicatePersonContactException(request.contact());
                }
            } catch (DuplicatePersonNameException | DuplicatePersonContactException e) {
                throw new InvalidPersonException("Line " + line.number() + ": " + e.getMessage());
            }
            matches.add(match);
        }
        String defaultGrade = settings.gradeForNew(sheet);
        int added = 0;
        int updated = 0;
        for (int i = 0; i < input.lines().size(); i++) {
            PersonRequest request = input.lines().get(i).person();
            if (request.grade() == null) {
                request = ValidateImport.withGrade(request, defaultGrade);
            }
            Person match = matches.get(i);
            Rollup before = rollups.take(sheet);
            if (match == null) {
                Person person = repository.insert(sheet, request.name(), request.grade(),
                        request.terms().isEmpty() ? null : Integer.valueOf(request.terms()),
                        request.contact().isEmpty() ? null : request.contact());
                AuditPersonAdded.record(request, person, audit);
                added++;
            } else {
                update(sheet, input.user(), request, repository.find(sheet, match.id()), repository, sheets, audit);
                updated++;
            }
            rollups.record(sheet, input.user(), before);
        }
        result.set(new ImportResponse(added, updated));
    }

    /** Edit the person as the form's edit would, keeping their name: a new version, the grade from the sheet's today,
     *  the terms and contact when the line gives them; then its PERSON_EDITED line. */
    private static void update(String sheet, String user, PersonRequest request, Person loaded,
            PersonRepository repository, SheetRepository sheets, Audit audit) throws InvalidPersonException {
        Person person = new Person(loaded.id(), loaded.sheetId(), loaded.name(), loaded.grade(), loaded.version() + 1,
                loaded.statementCount(), loaded.statedTotal(), loaded.statementDate(), loaded.retiredFrom(),
                loaded.grades(), loaded.holds(), loaded.terms(), loaded.contact(), loaded.aliases(), loaded.origin(),
                loaded.retirements(), loaded.tags(), loaded.statements());
        if (!repository.update(person, loaded.version())) {
            throw new InvalidPersonException(new StalePersonException(loaded.name()).getMessage());
        }
        LocalDate from = sheets.today(sheet);
        repository.grade(person, request.grade(), from);
        String terms = request.terms().isEmpty() ? null : request.terms();
        String contact = request.contact().isEmpty() ? null : request.contact();
        if (terms != null) {
            repository.terms(person, Integer.valueOf(terms));
        }
        if (contact != null) {
            repository.contact(person, contact);
        }
        PersonResponse saved = PersonResponse.from(repository.find(sheet, loaded.id()));
        audit.record("PERSON_EDITED sheet=" + sheet + " by=" + user + " name=" + saved.name()
                + " grade=" + saved.grade() + (terms != null ? " terms=" + terms : "")
                + (contact != null ? " contact=" + contact : ""));
    }
}

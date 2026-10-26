package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.BadRequestException;
import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.access.SettingRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Imports the lines, in order, a line with a blank grade taking the sheet's default grade. A line whose name is a person's name or alias on the sheet (as names are compared)
 * updates that person; any other line adds one. Every line is first checked as the person form checks it — an add as
 * a new person (name or alias in use on the sheet or its tree, contact another person's), an update as an edit keeping
 * the person's name (two lines naming the same person refuse it too); one refusal refuses them all before anything is
 * written. Each is then saved as the form saves it: an add writes PERSON_ADDED, an update gives the grade from the
 * sheet's today, sets the terms and contact the line gives (blank keeps them) and writes PERSON_EDITED; then the
 * rollup lines it moved.
 */
public class AddImportedPeople {

    public void add(@Val SheetResponse sheet, @Val ImportedPeople imported, @Val ChangedBy by,
            PersonRepository repository, SheetRepository sheets, SettingRepository settings, Rollup rollup, Audit audit,
            Out<ImportPeopleResponse> response) throws BadRequestException, ConflictException {
        String defaultGrade = settings.defaultGrade(sheet.id());
        List<PersonRequest> requests = imported.people().stream().map(r -> r.grade() != null ? r
                : new PersonRequest(r.name(), defaultGrade, r.by(), r.version(), r.gradeFrom(), r.terms(), r.contact(),
                        r.tags(), r.termsFrom())).toList();
        List<Person> matched = new ArrayList<>();
        Set<Long> seen = new HashSet<>();
        int n = 0;
        for (PersonRequest request : requests) {
            n++;
            Person person = repository.findByName(sheet.id(), request.name());
            if (person == null) {
                if (repository.nameTaken(sheet.id(), request.name(), null)) {
                    throw new BadRequestException("Line " + n + ": " + PersonRepository.NAME_TAKEN);
                }
                if (repository.contactTaken(sheet.id(), request.contactOrNull(), null)) {
                    throw new BadRequestException("Line " + n + ": " + PersonRepository.CONTACT_TAKEN);
                }
            } else {
                if (!seen.add(person.getId())) {
                    throw new BadRequestException("Line " + n + ": " + person.getName() + " is repeated in the import");
                }
                try {
                    new CheckPersonRename().check(sheet, edit(request, person), person, repository);
                } catch (BadRequestException e) {
                    throw new BadRequestException("Line " + n + ": " + e.getMessage());
                }
            }
            matched.add(person);
        }
        LocalDate today = sheets.today(sheet.id());
        int added = 0;
        int updated = 0;
        for (int i = 0; i < requests.size(); i++) {
            PersonRequest request = requests.get(i);
            Person person = matched.get(i);
            RollupSnapshot before = rollup.snapshot(sheet.id());
            if (person == null) {
                person = repository.insert(sheet.id(), request.name(), request.grade(), request.termsDays(),
                        request.contactOrNull());
                person.setToday(today);
                new RecordPersonAdded().record(person, by, audit);
                added++;
            } else {
                PersonRequest edit = edit(request, person);
                Integer termsBefore = person.getTerms();
                String contactBefore = person.getContact();
                person.setTerms(edit.termsDays());
                person.setContact(edit.contactOrNull());
                if (!repository.update(person)) {
                    throw new ConflictException(PersonRepository.CHANGED);
                }
                repository.giveGrade(person, edit.grade(), today, today);
                person.setToday(today);
                new RecordPersonEdited().record(person, by,
                        new TermsEdit(!Objects.equals(termsBefore, person.getTerms())),
                        new ContactEdit(!Objects.equals(contactBefore, person.getContact())), audit);
                updated++;
            }
            rollup.record(sheet.id(), before, by);
        }
        response.set(new ImportPeopleResponse(added, updated));
    }

    /** The line as the form's edit of the person: their name kept, and their terms and contact kept when blank. */
    private static PersonRequest edit(PersonRequest request, Person person) {
        String terms = request.termsDays() != null ? request.terms()
                : person.getTerms() == null ? "" : String.valueOf(person.getTerms());
        String contact = request.contactOrNull() != null ? request.contact()
                : person.getContact() == null ? "" : person.getContact();
        return new PersonRequest(person.getName(), request.grade(), request.by(), person.getVersion(), "", terms,
                contact, person.getTags() == null ? "" : person.getTags(), "");
    }
}

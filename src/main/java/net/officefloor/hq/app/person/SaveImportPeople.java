package net.officefloor.hq.app.person;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Takes the imported lines in order. A line whose name is a person's name or alias on the sheet (as names
 * are compared) updates that person as the person form saves them: the grade is recorded from the sheet's
 * today (nothing when it is the grade they have), the terms and contact are set when the line gives them,
 * audited as PERSON_EDITED with the figures it moved. Any other line adds a person as the person form adds
 * them (PERSON_ADDED, then the figures it moved). Every line is checked against the sheet first (an added
 * name clashes with no one's name or alias, a contact with no one else's contact, no person is named by two
 * lines), so one refused line writes nothing.
 */
public class SaveImportPeople {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidImportPeople request, People people, Audit audit,
            Rollup rollup, ObjectResponse<ImportPeopleResponse> response) throws InvalidPersonException, PersonConflictException {
        List<Person> matched = new ArrayList<>();
        Set<Long> updating = new HashSet<>();
        int number = 0;
        for (PersonRequest line : request.people()) {
            number++;
            Person person = people.findByName(sheetId, line.name());
            if (person == null) {
                if (people.nameTaken(sheetId, line.name(), null)) {
                    throw new InvalidPersonException("Person " + number + ": the name is already in use");
                }
            } else if (!updating.add(person.getId())) {
                throw new InvalidPersonException("Person " + number + ": " + person.getName() + " is named twice in the import");
            }
            if (!line.contact().isEmpty()
                    && people.contactTaken(sheetId, line.contact(), person == null ? null : person.getId())) {
                throw new InvalidPersonException("Person " + number + ": the contact is already in use");
            }
            matched.add(person);
        }
        int added = 0;
        int updated = 0;
        for (int i = 0; i < request.people().size(); i++) {
            PersonRequest line = request.people().get(i);
            Person person = matched.get(i);
            RollupFigures before = rollup.figures(sheetId);
            if (person == null) {
                person = people.add(sheetId, line.name(), line.grade(),
                        line.terms().isEmpty() ? null : Integer.valueOf(line.terms()),
                        line.contact().isEmpty() ? null : line.contact());
                audit.recordOnCommit("PERSON_ADDED sheet=" + sheetId + " by=" + request.user()
                        + " name=" + person.getName() + " grade=" + person.getGrade()
                        + (person.getTerms() == null ? "" : " terms=" + person.getTerms())
                        + (person.getContact() == null ? "" : " contact=" + person.getContact()));
                added++;
            } else {
                if (!line.terms().isEmpty()) {
                    person.editTerms(Integer.valueOf(line.terms()));
                }
                if (!line.contact().isEmpty()) {
                    person.editContact(line.contact());
                }
                if (!line.grade().equals(person.getGrade())) {
                    person.recordGrade(person.getToday(), line.grade());
                }
                if (!people.save(person)) {
                    throw new PersonConflictException(person.getId());
                }
                audit.recordOnCommit("PERSON_EDITED sheet=" + sheetId + " by=" + request.user()
                        + " name=" + person.getName() + " grade=" + person.getGrade()
                        + (person.isTermsChanged() ? " terms=" + (person.getTermsShown() == null ? "" : person.getTermsShown()) : "")
                        + (person.isContactChanged() ? " contact=" + (person.getContact() == null ? "" : person.getContact()) : ""));
                updated++;
            }
            rollup.recordChanges(sheetId, request.user(), before);
        }
        response.send(new ImportPeopleResponse(added, updated));
    }
}

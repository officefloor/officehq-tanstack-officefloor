package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, People people)
            throws InvalidPersonException, PersonConflictException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new PersonConflictException(person.getId());
        }
        // A person already clashing stays refused until renamed to a name that clashes nowhere in the tree (nor any alias).
        if (people.nameTaken(person.getSheetId(), request.name(), person.getId()) || people.wouldClash(person, request.name())) {
            throw new InvalidPersonException("The name is already in use");
        }
        if (!request.contact().isEmpty() && people.contactTaken(person.getSheetId(), request.contact(), person.getId())) {
            throw new InvalidPersonException("The contact is already in use");
        }
        person.setName(request.name());
        person.editTerms(request.terms().isEmpty() ? null : Integer.valueOf(request.terms()));
        person.editContact(request.contact().isEmpty() ? null : request.contact());
        person.setTags(request.tags().isEmpty() ? null : request.tags());
        // The form's grade is recorded from its from-date (the sheet's today when blank), keeping the
        // grades already recorded; a save leaving the grade as the row showed it with no from-date
        // records nothing.
        if (request.gradeFrom().isEmpty() && request.grade().equals(person.getGrade())) {
            return;
        }
        LocalDate from = request.gradeFrom().isEmpty() ? person.getToday() : LocalDate.parse(request.gradeFrom());
        person.recordGrade(from, request.grade());
    }
}

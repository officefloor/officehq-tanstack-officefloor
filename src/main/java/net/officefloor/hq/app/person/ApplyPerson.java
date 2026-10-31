package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, People people,
            net.officefloor.hq.app.grade.RetiredGrades grades)
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
        // A retired grade cannot be chosen: only a save leaving the person on it, with no from-date, keeps it.
        boolean recordsGrade = !request.gradeFrom().isEmpty() || !request.grade().equals(person.getGrade());
        if (recordsGrade) {
            grades.refuseRetired(person.getSheetId(), request.grade());
        }
        person.setName(request.name());
        // The form's terms are recorded from their from-date, keeping those already recorded; with no
        // from-date they are the terms from the beginning of time (blank: none).
        Integer terms = request.terms().isEmpty() ? null : Integer.valueOf(request.terms());
        if (request.termsFrom().isEmpty()) {
            person.editTerms(terms);
        } else {
            person.recordTerms(LocalDate.parse(request.termsFrom()), terms);
        }
        person.editContact(request.contact().isEmpty() ? null : request.contact());
        person.setTags(request.tags().isEmpty() ? null : request.tags());
        // The form's grade is recorded from its from-date (the sheet's today when blank), keeping the
        // grades already recorded; a save leaving the grade as the row showed it with no from-date
        // records nothing.
        if (!recordsGrade) {
            return;
        }
        LocalDate from = request.gradeFrom().isEmpty() ? person.getToday() : LocalDate.parse(request.gradeFrom());
        person.recordGrade(from, request.grade());
    }
}

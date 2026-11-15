package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, PersonRepository people,
            SheetRepository sheets)
            throws InvalidPersonException, PersonConflictException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new PersonConflictException(person.getId());
        }
        if (people.nameTaken(person.getSheetId(), request.name(), person.getId())) {
            throw new InvalidPersonException("The name is already in use");
        }
        if (request.contact() != null && people.contactTaken(person.getSheetId(), request.contact(), person.getId())) {
            throw new InvalidPersonException("The contact is already in use");
        }
        person.setName(request.name());
        person.giveContact(request.contact());
        person.giveTerms(request.terms() == null ? null : Integer.valueOf(request.terms()));
        // The form's grade is given from its from-date (blank: the sheet's today); a save leaving
        // the grade as the row showed it, with no from-date, gives none.
        if (request.gradeFrom() != null || !request.grade().equals(person.getGrade())) {
            java.time.LocalDate today = sheets.today(person.getSheetId());
            person.giveGrade(request.grade(), request.gradeFrom() != null ? request.gradeFrom() : today.toString(), today);
        }
    }
}

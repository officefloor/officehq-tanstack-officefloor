package net.officefloor.hq.app.person;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Changes the person's name and records the form's grade from its from-date (blank: the sheet's
 *  today), keeping the grades already recorded — except that a save leaving the grade as the row
 *  showed it (the one in force on the sheet's today), with the from-date blank, records none.
 *  The person's own terms and contact become the form's (blank: none). */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, People people, Sheets sheets)
            throws InvalidSheetRequestException, PersonConflictException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new PersonConflictException(PersonResponse.from(person));
        }
        if (people.nameTaken(person.getSheetId(), request.name(), person.getId())) {
            throw new InvalidSheetRequestException("The name is already in use on this sheet");
        }
        if (request.contactKey() != null
                && people.contactTaken(person.getSheetId(), request.contactKey(), person.getId())) {
            throw new InvalidSheetRequestException("The contact is already in use on this sheet");
        }
        person.setName(request.name());
        person.changeContact(request.contactKey());
        person.changeTerms(request.termsDays());
        var from = request.gradeFromDate();
        var today = sheets.today(person.getSheetId());
        person.setToday(today);
        if (from == null && request.grade().equals(person.getGrade())) return;
        person.recordGrade(request.grade(), from != null ? from : today);
    }
}

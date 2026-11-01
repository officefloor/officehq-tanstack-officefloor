package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Val;

/** Changes the person's name and records the form's grade from its from-date (blank: the sheet's
 *  today), keeping the grades already recorded — except that a save leaving the grade as the row
 *  showed it (the one in force on the sheet's today), with the from-date blank, records none.
 *  The person's own terms are recorded from the form's terms from-date, keeping those already recorded
 *  (blank from-date: from the beginning of time). Their contact and tags become the form's (blank:
 *  none; tags not given: kept). */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, People people, Sheets sheets)
            throws InvalidSheetRequestException, PersonConflictException {
        if (request.version() != null && request.version() != person.getVersion()) {
            throw new PersonConflictException(PersonResponse.from(person));
        }
        check(request, person, people);
        applied(request, person, sheets);
    }

    /** Refused when the edit's name, an alias of the person, or the edit's contact is taken. */
    static void check(PersonRequest request, Person person, People people) throws InvalidSheetRequestException {
        if (people.nameUsedInTree(person.getSheetId(), request.name(), person.getId())) {
            throw new InvalidSheetRequestException("The name is already in use on this sheet or its tree");
        }
        // A person whose alias clashes with its tree cannot be edited until no alias clashes.
        Person renamed = new Person(person.getId(), person.getSheetId(), request.name(), person.getGrade());
        renamed.setAliases(person.getAliases());
        if (people.clashing(renamed)) {
            throw new InvalidSheetRequestException("An alias of this person is in use on another sheet of its tree");
        }
        if (request.contactKey() != null
                && people.contactTaken(person.getSheetId(), request.contactKey(), person.getId())) {
            throw new InvalidSheetRequestException("The contact is already in use on this sheet");
        }
    }

    /** Makes the checked edit to the person, ready to be saved. */
    static void applied(PersonRequest request, Person person, Sheets sheets) {
        person.setName(request.name());
        person.changeContact(request.contactKey());
        if (request.tags() != null) person.setTags(request.tagsKey());
        LocalDate termsFrom = request.termsFromDate();
        if (termsFrom != null) person.recordTerms(request.termsDays(), termsFrom);
        else person.changeTerms(request.termsDays());
        LocalDate from = request.gradeFromDate();
        LocalDate today = sheets.today(person.getSheetId());
        person.setToday(today);
        if (from == null && request.grade().equals(person.getGrade())) return;
        person.recordGrade(request.grade(), from != null ? from : today);
    }
}

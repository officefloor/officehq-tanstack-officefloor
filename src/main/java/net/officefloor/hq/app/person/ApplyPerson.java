package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Applies the edit to the person as loaded; refused when the editor loaded an older version. The form's grade
 * is recorded from its from-date (blank: the sheet's today), keeping the grades already recorded — except that a
 * grade left as the row showed it, with the from-date blank, records nothing. The person's own terms are recorded
 * likewise from the terms' from-date (blank: from the beginning of time; blank terms: none), keeping those recorded
 * from other dates — except that terms left as the row showed them, with the from-date blank, record nothing. Their
 * contact becomes the form's; whether terms were recorded (even the same as those already recorded) or the contact
 * changed is published for the audit.
 */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, SheetRepository sheets, Out<Person> updated,
            Out<PersonTermsChange> termsChange) throws StalePersonException {
        if (request.version() != null && request.version() != person.version()) {
            throw new StalePersonException(person.name());
        }
        LocalDate today = sheets.today(person.sheetId());
        Integer terms = request.termsDays();
        LocalDate termsFrom = request.termsFromDate();
        boolean recorded = termsFrom != null || !Objects.equals(terms, person.terms());
        Person termed = recorded ? person.withTerms(terms, termsFrom, today) : person;
        String contact = request.contactOrNull();
        termsChange.set(new PersonTermsChange(recorded,
                !Objects.equals(contact, person.contact())));
        boolean blank = request.gradeFrom() == null || request.gradeFrom().isEmpty();
        if (blank && request.grade().equals(person.grade())) {
            updated.set(termed.withName(request.name()).withContact(contact));
            return;
        }
        LocalDate from = blank ? today : LocalDate.parse(request.gradeFrom());
        updated.set(termed.withNameAndGrade(request.name(), request.grade(), from, today).withContact(contact));
    }
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import java.util.Objects;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Applies the edit to the person as loaded; refused when the editor loaded an older version. The form's grade
 * is recorded from its from-date (blank: the sheet's today), keeping the grades already recorded — except that a
 * grade left as the row showed it, with the from-date blank, records nothing. The person's own terms become the
 * form's (blank: none), as does their contact; whether either changed is published for the audit.
 */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, SheetRepository sheets, Out<Person> updated,
            Out<PersonTermsChange> termsChange) throws StalePersonException {
        if (request.version() != null && request.version() != person.version()) {
            throw new StalePersonException(person.name());
        }
        Integer terms = request.termsDays();
        String contact = request.contactOrNull();
        termsChange.set(new PersonTermsChange(!Objects.equals(terms, person.terms()),
                !Objects.equals(contact, person.contact())));
        boolean blank = request.gradeFrom() == null || request.gradeFrom().isEmpty();
        if (blank && request.grade().equals(person.grade())) {
            updated.set(person.withName(request.name()).withTerms(terms).withContact(contact));
            return;
        }
        LocalDate today = sheets.today(person.sheetId());
        LocalDate from = blank ? today : LocalDate.parse(request.gradeFrom());
        updated.set(person.withNameAndGrade(request.name(), request.grade(), from, today).withTerms(terms)
                .withContact(contact));
    }
}

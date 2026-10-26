package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Objects;
import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Saves the edit, refusing it (nothing written) when the person was saved by someone else since
 *  the form loaded it. The grade is given from the request's from-date (the sheet's today when blank), keeping the
 *  grades already given; the person then shows the grade in force on the sheet's today. Their own terms are given
 *  from the request's terms from-date (replacing those from that same date, keeping the rest), or when blank replace
 *  those from the beginning of time, publishing whether any changed, and likewise their contact; their tags are replaced too. */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, PersonRepository repository,
            SheetRepository sheets, Out<TermsEdit> termsEdit, Out<ContactEdit> contactEdit) throws ConflictException {
        if (request.version() == null || request.version() != person.getVersion()) {
            throw new ConflictException(PersonRepository.CHANGED);
        }
        Integer before = person.getTerms();
        java.util.Set<TermsFrom> datedBefore = java.util.Set.copyOf(person.getDatedTerms());
        LocalDate termsFrom = request.termsFromDate();
        person.setName(request.name());
        if (termsFrom == null) {
            person.setTerms(request.termsDays());
        }
        String contactBefore = person.getContact();
        person.setContact(request.contactOrNull());
        person.setTags(request.tagsOrNull());
        contactEdit.set(new ContactEdit(!Objects.equals(contactBefore, person.getContact())));
        if (!repository.update(person)) {
            throw new ConflictException(PersonRepository.CHANGED);
        }
        LocalDate today = sheets.today(person.getSheetId());
        LocalDate from = request.gradeFrom() == null || request.gradeFrom().isEmpty() ? today
                : LocalDate.parse(request.gradeFrom());
        repository.giveGrade(person, request.grade(), from, today);
        if (termsFrom != null) {
            repository.giveTerms(person, request.termsDays(), termsFrom);
        }
        person.setToday(today);
        termsEdit.set(new TermsEdit(!Objects.equals(before, person.getTerms())
                || !datedBefore.equals(java.util.Set.copyOf(person.getDatedTerms()))));
    }
}

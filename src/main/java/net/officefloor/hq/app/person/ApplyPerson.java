package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Applies the edit: the name, and the form's grade from its from-date (the sheet's today when blank),
 * keeping the grades already recorded. A grade left as the row showed it (the one in force on the
 * sheet's today) with the from-date blank records nothing. The form's terms are recorded from its
 * terms from-date, keeping those already recorded; with the from-date blank they become the terms from
 * the beginning of time, but terms left as the row showed them (those in force on the sheet's today)
 * record nothing. A retired grade is refused unless it is the one in force, left as it was with the from-date blank. Whether the form's terms differ from those the row showed is published for the audit; likewise their contact. Their tags become the form's.
 */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, @Val Sheet sheet, GradeRepository grades,
            Out<TermsEdit> termsEdit, Out<ContactEdit> contactEdit) throws InvalidPersonException {
        boolean kept = request.gradeFrom().isEmpty() && request.grade().equals(person.gradeOn(sheet.today()));
        if (!kept && grades.isRetired(sheet.id(), request.grade())) throw RetiredGrades.refused(request.grade());
        person.setName(request.name());
        Integer terms = request.termsDays();
        boolean termsShown = java.util.Objects.equals(terms, person.termsOn(sheet.today()));
        termsEdit.set(new TermsEdit(!termsShown));
        if (!request.termsFrom().isEmpty()) person.setTermsFrom(terms, LocalDate.parse(request.termsFrom()));
        else if (!termsShown) person.setTerms(terms);
        String contact = request.contactValue();
        contactEdit.set(new ContactEdit(!java.util.Objects.equals(contact, person.getContact())));
        person.setContact(contact);
        person.setTags(request.tagsValue());
        boolean dated = !request.gradeFrom().isEmpty();
        if (!dated && request.grade().equals(person.gradeOn(sheet.today()))) return;
        person.setGradeFrom(request.grade(), dated ? LocalDate.parse(request.gradeFrom()) : sheet.today());
    }
}

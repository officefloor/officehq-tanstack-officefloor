package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Gives the person the row's grade from the sheet's today exactly as the person form would (see
 * {@link ApplyPerson}) with a blank from-date: the grades already recorded are kept, and a grade left
 * as the row shows it records nothing. Their name, terms, contact and tags are untouched.
 */
public class ApplyInlineGrade {
    public void apply(@Val InlineGradeRequest request, @Val Person person, @Val Sheet sheet, GradeRepository grades,
            Out<TermsEdit> termsEdit, Out<ContactEdit> contactEdit) throws InvalidPersonException {
        termsEdit.set(new TermsEdit(false));
        contactEdit.set(new ContactEdit(false));
        if (request.grade().equals(person.gradeOn(sheet.today()))) return;
        if (grades.isRetired(sheet.id(), request.grade())) throw RetiredGrades.refused(request.grade());
        person.setGradeFrom(request.grade(), sheet.today());
    }
}

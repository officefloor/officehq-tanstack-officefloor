package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val PersonRequest request, @Val Sheet sheet, GradeRepository grades, Out<Person> personOut)
            throws InvalidPersonException {
        if (!grades.exists(sheet.id(), request.grade())) throw RetiredGrades.unknown(request.grade());
        if (grades.isRetired(sheet.id(), request.grade())) throw RetiredGrades.refused(request.grade());
        Person person = new Person(null, sheet.id(), request.name(), request.grade());
        if (request.termsFrom().isEmpty()) person.setTerms(request.termsDays());
        else person.setTermsFrom(request.termsDays(), java.time.LocalDate.parse(request.termsFrom()));
        person.setContact(request.contactValue());
        person.setTags(request.tagsValue());
        personOut.set(person);
    }
}

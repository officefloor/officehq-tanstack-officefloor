package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

public class BuildPerson {
    public void build(@Val PersonRequest request, @Val Sheet sheet, GradeRepository grades, Out<Person> person)
            throws InvalidPersonException {
        grades.checkChoosable(sheet.id(), request.grade());
        Person built = new Person(null, sheet.id(), request.name(), request.grade(), 0, 0, null, null, null, List.of())
                .withContact(request.contactOrNull());
        // terms with a from-date are recorded from it, none of their own before it
        person.set(request.termsFromDate() == null ? built.withTerms(request.termsDays())
                : built.withTerms(request.termsDays(), request.termsFromDate(),
                        sheet.today() == null ? java.time.LocalDate.now() : sheet.today()));
    }
}

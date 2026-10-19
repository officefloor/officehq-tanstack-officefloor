package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.plugin.variable.Val;

/** Renames the person and records the grade from the form's from-date, keeping the grades already recorded. */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person) {
        person.setName(request.name());
        person.gradeFrom(request.grade(), LocalDate.parse(request.gradeFrom()));
    }
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Val;

/** Applies the edit: the name, and the grade from its from-date on (none given: the sheet's today). */

public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, SheetRepository sheets) {
        person.setName(request.name());
        LocalDate from = request.gradeFromDate();
        person.recordGrade(request.grade(),
                from != null ? from : sheets.findById(person.getSheetId()).orElseThrow().getToday());
    }
}

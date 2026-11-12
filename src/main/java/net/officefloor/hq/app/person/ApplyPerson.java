package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Applies an edit: the name, and the form's grade recorded from its from-date (the sheet's today when
 *  blank), keeping the grades already recorded — except that a grade left as the row showed it (the one
 *  in force on the sheet's today) with the from-date blank records nothing. Their own terms become the form's
 *  (none when blank). */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, SheetStore sheets) {
        person.setName(request.name());
        person.setTerms(request.terms() == null ? null : Integer.valueOf(request.terms()));
        LocalDate today = sheets.today(person.getSheetId());
        if (request.gradeFrom() == null && request.grade().equals(person.gradeOn(today))) {
            return;
        }
        person.recordGrade(request.gradeFrom() == null ? today : LocalDate.parse(request.gradeFrom()),
                request.grade());
    }
}

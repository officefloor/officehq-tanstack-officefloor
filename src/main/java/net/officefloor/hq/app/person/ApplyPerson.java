package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Val;

/** Applies an edit: the name, and the form's grade recorded from its from-date (the sheet's today when
 *  blank), keeping the grades already recorded — except that a grade left as the row showed it (the one
 *  in force on the sheet's today) with the from-date blank records nothing. Their own terms and contact become the form's
 *  (none when blank). */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, SheetStore sheets) {
        person.setName(request.name());
        person.setContact(request.contact());
        person.setTerms(request.terms() == null ? null : Integer.valueOf(request.terms()));
        applyGrade(person, request.grade(), request.gradeFrom(), sheets.today(person.getSheetId()));
    }

    /** Records the grade from the from-date (yyyy-MM-dd, null for the sheet's today), as an edit does: nothing
     *  when the from-date is null and the grade is the one in force on the sheet's today. */
    static void applyGrade(Person person, String grade, String gradeFrom, LocalDate today) {
        if (gradeFrom == null && grade.equals(person.gradeOn(today))) {
            return;
        }
        person.recordGrade(gradeFrom == null ? today : LocalDate.parse(gradeFrom), grade);
    }
}

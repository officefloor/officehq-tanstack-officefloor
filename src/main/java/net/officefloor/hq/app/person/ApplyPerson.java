package net.officefloor.hq.app.person;

import java.time.LocalDate;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Applies the edit to the person as loaded; refused when the editor loaded an older version. The form's grade
 * is recorded from its from-date (blank: the sheet's today), keeping the grades already recorded — except that a
 * grade left as the row showed it, with the from-date blank, records nothing.
 */
public class ApplyPerson {
    public void apply(@Val PersonRequest request, @Val Person person, SheetRepository sheets, Out<Person> updated)
            throws StalePersonException {
        if (request.version() != null && request.version() != person.version()) {
            throw new StalePersonException(person.name());
        }
        boolean blank = request.gradeFrom() == null || request.gradeFrom().isEmpty();
        if (blank && request.grade().equals(person.grade())) {
            updated.set(person.withName(request.name()));
            return;
        }
        LocalDate today = sheets.today(person.sheetId());
        LocalDate from = blank ? today : LocalDate.parse(request.gradeFrom());
        updated.set(person.withNameAndGrade(request.name(), request.grade(), from, today));
    }
}

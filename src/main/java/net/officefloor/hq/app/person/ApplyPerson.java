package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Applies the edit: the grade is recorded from the from-date (the sheet's today when blank), replacing one recorded
 *  from the same date and keeping the others; a save leaving the grade as the row showed it (in force on the sheet's
 *  today) with the from-date blank records nothing.
 *  The person's own terms become the ones given (null: none), and their contact the one given (null: none). */
public class ApplyPerson {
    public void apply(@Val Person person, @Val PersonValues values, SheetRepository sheets, Out<Person> changed,
            Out<TermsBefore> termsBefore, Out<ContactBefore> contactBefore) {
        LocalDate today = sheets.today(person.sheetId());
        List<PersonGrade> grades = new ArrayList<>(person.grades());
        if (values.gradeFrom() != null || !values.grade().equals(person.grade())) {
            LocalDate from = values.gradeFrom() != null ? values.gradeFrom() : today;
            grades.removeIf((g) -> Objects.equals(g.from(), from));
            grades.add(new PersonGrade(values.grade(), from));
            grades.sort(Comparator.comparing(PersonGrade::from, Comparator.nullsFirst(Comparator.naturalOrder())));
        }
        termsBefore.set(new TermsBefore(person.terms()));
        contactBefore.set(new ContactBefore(person.contact()));
        List<PersonGrade> kept = List.copyOf(grades);
        changed.set(new Person(person.id(), person.sheetId(), values.name(), Person.gradeOn(kept, today), person.version() + 1,
                person.retiredFrom(), kept, person.holds(), values.terms(), values.contact(), person.aliases()));
    }
}

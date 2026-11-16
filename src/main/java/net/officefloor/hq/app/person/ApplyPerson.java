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
 *  The person's own terms given (null: none) are recorded from their from-date (the beginning of time when blank),
 *  replacing ones recorded from the same date and keeping the others; a save leaving them as the row showed them (in
 *  force on the sheet's today) with the from-date blank records nothing. Their contact becomes the one given (null: none), and their tags the ones given. */
public class ApplyPerson {
    public void apply(@Val Person person, @Val PersonValues values, SheetRepository sheets, Out<Person> changed,
            Out<TermsBefore> termsBefore, Out<ContactBefore> contactBefore) {
        termsBefore.set(new TermsBefore(person.terms(), person.ownTerms()));
        contactBefore.set(new ContactBefore(person.contact()));
        changed.set(edited(person, values, sheets.today(person.sheetId())));
    }

    /** The person as the edit leaves them, on the sheet's today (see above). */
    static Person edited(Person person, PersonValues values, LocalDate today) {
        List<PersonGrade> grades = new ArrayList<>(person.grades());
        if (values.gradeFrom() != null || !values.grade().equals(person.grade())) {
            LocalDate from = values.gradeFrom() != null ? values.gradeFrom() : today;
            grades.removeIf((g) -> Objects.equals(g.from(), from));
            grades.add(new PersonGrade(values.grade(), from));
            grades.sort(Comparator.comparing(PersonGrade::from, Comparator.nullsFirst(Comparator.naturalOrder())));
        }
        List<PersonGrade> kept = List.copyOf(grades);
        List<PersonTerms> own = new ArrayList<>(person.ownTerms());
        if (values.termsFrom() != null || !Objects.equals(values.terms(), person.terms())) {
            LocalDate from = values.termsFrom();
            own.removeIf((t) -> Objects.equals(t.from(), from));
            own.add(new PersonTerms(values.terms(), from));
            own.sort(Comparator.comparing(PersonTerms::from, Comparator.nullsFirst(Comparator.naturalOrder())));
        }
        List<PersonTerms> terms = List.copyOf(own);
        return new Person(person.id(), person.sheetId(), values.name(), Person.gradeOn(kept, today), person.version() + 1,
                person.retiredFrom(), kept, person.holds(), Person.termsOn(terms, today), values.contact(), person.aliases())
                .withRetirements(person.retirements()).withTags(values.tags()).withOwnTerms(terms, Person.termsOn(terms, today));
    }
}

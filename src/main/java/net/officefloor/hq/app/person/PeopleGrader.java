package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.sheet.SheetRepository;

/** The steps of the person form's edit that give people a grade (see GradePeople and the grade merge). */
public final class PeopleGrader {

    private PeopleGrader() {
    }

    /** Refuses the grade when retired on the sheet and chosen for any of the people (see CheckPersonRegrade). */
    public static void check(String sheet, Collection<Long> personIds, String grade, String by, LocalDate from,
            PersonRepository people, GradeRepository grades) throws InvalidPersonException {
        Set<Long> selected = new HashSet<>(personIds);
        for (Person listed : people.list(sheet)) {
            if (selected.contains(listed.id())) {
                CheckPersonRegrade.refuse(sheet, listed, new PersonValues(listed.name(), grade, by,
                        listed.version(), from, listed.terms(), listed.contact(), listed.tags()), grades);
            }
        }
    }

    /** Gives each of the people on the sheet the grade from the date (null: the sheet's today), in list order, as the
     *  person form's edit would; returns how many were changed. */
    public static int give(String sheet, Collection<Long> personIds, String grade, String by, LocalDate from,
            PersonRepository people, SheetRepository sheets, Rollup rollup, Audit audit)
            throws DuplicatePersonNameException, DuplicatePersonContactException, StalePersonException, InvalidPersonException {
        Set<Long> selected = new HashSet<>(personIds);
        ApplyPerson apply = new ApplyPerson();
        SavePerson save = new SavePerson();
        RecordPersonEdited record = new RecordPersonEdited();
        int graded = 0;
        for (Person listed : people.list(sheet)) {
            if (!selected.contains(listed.id())) {
                continue;
            }
            Person person = people.find(sheet, listed.id());
            if (person == null) {
                continue;
            }
            PersonValues values = new PersonValues(person.name(), grade, by, person.version(),
                    from, person.terms(), person.contact(), person.tags());
            RollupBefore before = rollup.take(sheet);
            Person[] changed = new Person[1];
            TermsBefore[] terms = new TermsBefore[1];
            ContactBefore[] contact = new ContactBefore[1];
            apply.apply(person, values, sheets, (p) -> changed[0] = p, (t) -> terms[0] = t, (c) -> contact[0] = c);
            save.save(changed[0], values, people);
            record.record(changed[0], values, terms[0], contact[0], before, rollup, audit);
            graded++;
        }
        return graded;
    }
}

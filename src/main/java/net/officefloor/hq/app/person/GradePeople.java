package net.officefloor.hq.app.person;

import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Gives each selected person the grade, in list order (see PersonRepository#list), through the very steps of the
 * person form's edit, made from the person's current version with their name, terms and contact kept: ApplyPerson (the grade
 * recorded from the from-date), SavePerson and RecordPersonEdited (PERSON_EDITED and the figures it moved). One not on
 * the sheet is skipped. A grade retired on the sheet chosen for any of them (see CheckPersonRegrade) refuses them all.
 */
public class GradePeople {
    public void grade(@PathVariable("sheetId") String sheetId, @Val BulkGradeValues request, PersonRepository people,
            SheetRepository sheets, Rollup rollup, Audit audit, GradeRepository grades,
            Out<BulkGradeResult> result)
            throws DuplicatePersonNameException, DuplicatePersonContactException, StalePersonException, InvalidPersonException {
        String sheet = sheetId.trim();
        Set<Long> selected = new HashSet<>(request.personIds());
        ApplyPerson apply = new ApplyPerson();
        SavePerson save = new SavePerson();
        RecordPersonEdited record = new RecordPersonEdited();
        for (Person listed : people.list(sheet)) {
            if (selected.contains(listed.id())) {
                CheckPersonRegrade.refuse(sheet, listed, new PersonValues(listed.name(), request.grade(), request.by(),
                        listed.version(), request.from(), listed.terms(), listed.contact(), listed.tags()), grades);
            }
        }
        int graded = 0;
        for (Person listed : people.list(sheet)) {
            if (!selected.contains(listed.id())) {
                continue;
            }
            Person person = people.find(sheet, listed.id());
            if (person == null) {
                continue;
            }
            PersonValues values = new PersonValues(person.name(), request.grade(), request.by(), person.version(),
                    request.from(), person.terms(), person.contact(), person.tags());
            RollupBefore before = rollup.take(sheet);
            Person[] changed = new Person[1];
            TermsBefore[] terms = new TermsBefore[1];
            ContactBefore[] contact = new ContactBefore[1];
            apply.apply(person, values, sheets, (p) -> changed[0] = p, (t) -> terms[0] = t, (c) -> contact[0] = c);
            save.save(changed[0], values, people);
            record.record(changed[0], values, terms[0], contact[0], before, rollup, audit);
            graded++;
        }
        result.set(new BulkGradeResult(graded));
    }
}

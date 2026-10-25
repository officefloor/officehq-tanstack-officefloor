package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.ConflictException;
import net.officefloor.hq.app.access.RoleRepository;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupSnapshot;
import net.officefloor.hq.app.session.ChangedBy;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.hq.app.sheet.SheetResponse;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/**
 * Gives each selected person the grade in list order — the people in the order they were added — exactly as saving
 * it on the person form would: the grade from the from-date (the sheet's today when blank), their name and terms kept,
 * a PERSON_EDITED line and the rollup lines it moved. A selected id not on this sheet is skipped. A viewer changes no one.
 */
public class GradePeople {
    public void grade(@Val SheetResponse sheet, @Val BulkGrade bulk, @Val ChangedBy by, PersonRepository people,
            SheetRepository sheets, RoleRepository roles, Rollup rollup, Audit audit, Out<BulkGradeResponse> result)
            throws ConflictException {
        if (roles.isViewer(sheet.id(), by.name())) {
            result.set(new BulkGradeResponse(0));
            return;
        }
        ApplyPerson apply = new ApplyPerson();
        RecordPersonEdited record = new RecordPersonEdited();
        int changed = 0;
        for (Person person : people.list(sheet.id())) {
            if (!bulk.ids().contains(person.getId())) {
                continue;
            }
            PersonRequest request = new PersonRequest(person.getName(), bulk.grade(), by.name(), person.getVersion(),
                    bulk.from(), person.getTerms() == null ? "" : String.valueOf(person.getTerms()));
            RollupSnapshot before = rollup.snapshot(sheet.id());
            TermsEdit[] terms = new TermsEdit[1];
            apply.apply(request, person, people, sheets, value -> terms[0] = value);
            record.record(person, by, terms[0], audit);
            rollup.record(sheet.id(), before, by);
            changed++;
        }
        result.set(new BulkGradeResponse(changed));
    }
}

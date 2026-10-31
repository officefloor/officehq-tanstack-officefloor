package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.settings.Role;
import net.officefloor.hq.app.settings.Settings;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Gives each selected person the grade from the from-date, in list order, exactly as saving the
 *  person form would (their name and terms as they are): each audited PERSON_EDITED, then the
 *  figures it moved. One not on the sheet is skipped. A viewer, or anyone on an archived sheet,
 *  grades no one. */
public class GradePeople {
    public void grade(@PathVariable("sheetId") String sheetId, @Val BulkGrade bulk, People people, Sheets sheets,
            Rollup rollup, Settings settings, Audit audit, Out<BulkGradeResult> result) {
        if (sheets.archived(sheetId) || settings.role(sheetId, bulk.user()) == Role.VIEWER) {
            result.set(new BulkGradeResult(0));
            return;
        }
        ApplyPerson apply = new ApplyPerson();
        SavePerson save = new SavePerson();
        RecordPersonEdited record = new RecordPersonEdited();
        int graded = 0;
        for (Person listed : people.onSheet(sheetId)) {
            if (!bulk.personIds().contains(listed.getId())) continue;
            Person person = people.find(sheetId, listed.getId());
            if (person == null) continue;
            PersonRequest request = new PersonRequest(person.getName(), bulk.grade(), bulk.user(), null, bulk.from(),
                    person.getTerms() == null ? "" : String.valueOf(person.getTerms()));
            Snapshot before = rollup.snapshot(sheetId);
            try {
                apply.apply(request, person, people, sheets);
                save.save(person, people);
            } catch (InvalidSheetRequestException | PersonConflictException e) {
                continue;
            }
            record.record(request, person, audit);
            rollup.record(sheetId, before, bulk.user());
            graded++;
        }
        result.set(new BulkGradeResult(graded));
    }
}

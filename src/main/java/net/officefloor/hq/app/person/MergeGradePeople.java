package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.grade.GradeMerge;
import net.officefloor.hq.app.grade.GradeMergeResult;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Snapshot;
import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.hq.app.sheet.Sheets;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Audits the merge, then gives every person whose grade in force on the sheet's today is the merged
 *  grade the other one from the sheet's today, in list order, exactly as saving the person form would
 *  (their name and terms as they are): each audited PERSON_EDITED, then the figures it moved. Last,
 *  the figures the whole merge moved. */
public class MergeGradePeople {
    public void merge(@Val GradeMerge merge, People people, Sheets sheets, Rollup rollup, Audit audit,
            Grades grades, Out<GradeMergeResult> result) {
        audit.record("GRADE_MERGED sheet=" + merge.sheetId() + " by=" + merge.by()
                + " from=" + merge.from() + " into=" + merge.into());
        Snapshot start = rollup.snapshot(merge.sheetId());
        ApplyPerson apply = new ApplyPerson();
        SavePerson save = new SavePerson();
        RecordPersonEdited record = new RecordPersonEdited();
        int moved = 0;
        for (Person listed : people.onSheet(merge.sheetId())) {
            Person person = people.find(merge.sheetId(), listed.getId());
            if (person == null) continue;
            person.setToday(sheets.today(merge.sheetId()));
            if (!merge.from().equals(person.getGrade())) continue;
            PersonRequest request = new PersonRequest(person.getName(), merge.into(), merge.by(), null, "",
                    person.getTerms() == null ? "" : String.valueOf(person.getTerms()),
                    person.getContact());
            Snapshot before = rollup.snapshot(merge.sheetId());
            try {
                apply.apply(request, person, people, sheets, grades);
                save.save(person, people);
            } catch (InvalidSheetRequestException | PersonConflictException e) {
                continue;
            }
            record.record(request, person, audit);
            rollup.record(merge.sheetId(), before, merge.by());
            moved++;
        }
        rollup.record(merge.sheetId(), start, merge.by());
        result.set(new GradeMergeResult(merge.from(), merge.into(), moved));
    }
}

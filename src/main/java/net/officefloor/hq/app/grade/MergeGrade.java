package net.officefloor.hq.app.grade;

import java.time.LocalDate;
import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRules;
import net.officefloor.hq.app.person.PersonStore;
import net.officefloor.hq.app.person.StalePersonException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupService;
import net.officefloor.hq.app.sheet.InvalidSheetException;
import net.officefloor.hq.app.sheet.SheetStore;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records GRADE_MERGED, then gives every person on the merged grade (in force on the sheet's today) the other
 *  grade from the sheet's today, in list order, exactly as the person form's edit would, each recorded as
 *  PERSON_EDITED and then how it moved the sheet's figures. The merge is retroactive: every grade record of each
 *  person moved that is on the merged grade then becomes the other, whatever its date. Then how the merge as a
 *  whole moved the figures. */
public class MergeGrade {
    public void merge(@PathVariable("sheetId") String sheetId, @Val GradeMergeInput input, PersonStore store,
            SheetStore sheets, RollupService rollups, Audit audit, GradeStore grades, Out<GradeMergeResult> result)
            throws InvalidSheetException, StalePersonException {
        String sheet = sheetId.trim();
        LocalDate today = sheets.today(sheet);
        List<Person> moved = store.list(sheet).stream().filter(p -> input.from().equals(p.gradeOn(today))).toList();
        for (Person person : moved) {
            grades.refuseChoice(sheet, input.into(), person, null, today);
        }
        audit.record("GRADE_MERGED sheet=" + sheet + " by=" + input.user() + " from=" + input.from() + " into="
                + input.into());
        Rollup merging = rollups.snapshot(sheet);
        for (Person person : moved) {
            Rollup before = rollups.snapshot(sheet);
            PersonRules.applyGrade(person, input.into(), null, today);
            if (!store.save(person)) {
                throw new StalePersonException();
            }
            audit.record("PERSON_EDITED sheet=" + sheet + " by=" + input.user() + " name=" + person.getName()
                    + " grade=" + person.gradeOn(today));
            rollups.record(sheet, input.user(), before);
        }
        for (Person person : moved) {
            store.regrade(person, input.from(), input.into());
        }
        rollups.record(sheet, input.user(), merging);
        result.set(new GradeMergeResult(input.from(), input.into(), moved.size()));
    }
}

package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.DuplicatePersonContactException;
import net.officefloor.hq.app.person.DuplicatePersonNameException;
import net.officefloor.hq.app.person.PeopleGrader;
import net.officefloor.hq.app.person.InvalidPersonException;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonRepository;
import net.officefloor.hq.app.person.StalePersonException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.hq.app.sheet.SheetRepository;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Merges the grade into the other: writes GRADE_MERGED, then gives every person whose grade in force on the sheet's
 * today is the merged grade the other from the sheet's today, in list order, each exactly as the person form would (see
 * PeopleGrader); then the figures the whole merge moved (see Rollup). The other grade retired on the sheet, when anyone
 * is to be given it, refuses the merge before anything is written.
 */
public class MergeGrade {
    public void merge(@PathVariable("sheetId") String sheetId, @Val MergeGradeValues values, PersonRepository people,
            SheetRepository sheets, Rollup rollup, Audit audit, GradeRepository grades, Out<MergeGradeResult> result)
            throws DuplicatePersonNameException, DuplicatePersonContactException, StalePersonException,
            InvalidPersonException {
        String sheet = sheetId.trim();
        List<Long> ids = people.list(sheet).stream().filter((p) -> values.from().equals(p.grade())).map(Person::id).toList();
        PeopleGrader.check(sheet, ids, values.into(), values.by(), null, people, grades);
        audit.record("GRADE_MERGED sheet=" + sheet + " by=" + values.by() + " from=" + values.from() + " into="
                + values.into());
        RollupBefore before = rollup.take(sheet);
        int merged = PeopleGrader.give(sheet, ids, values.into(), values.by(), null, people, sheets, rollup, audit);
        rollup.record(sheet, values.by(), before);
        result.set(new MergeGradeResult(merged));
    }
}

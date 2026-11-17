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
        PeopleGrader.check(sheet, request.personIds(), request.grade(), request.by(), request.from(), people, grades);
        result.set(new BulkGradeResult(PeopleGrader.give(sheet, request.personIds(), request.grade(), request.by(), request.from(),
                people, sheets, rollup, audit)));
    }
}

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Gives the selected people the grade in list order (the order they were added), each exactly as a
 * person form save would: recorded from the from-date (the sheet's today when blank), audited as
 * PERSON_EDITED with the rollup lines it moved, and counted. As on the form, a blank from-date leaving a
 * person's grade as it is records no grade, though the person is still saved, audited and counted. A
 * selected id that is not a person on the sheet is skipped.
 */
public class BulkGradePeople {
    public void grade(@PathVariable("sheetId") String sheetId, @Val BulkGradeRequest request,
            People people, Rollup rollup, Audit audit, net.officefloor.hq.app.grade.RetiredGrades grades,
            ObjectResponse<BulkGradeResponse> response) throws PersonConflictException, InvalidPersonException {
        Set<String> selected = new HashSet<>(request.personIds());
        int graded = 0;
        // A retired grade cannot be chosen: refused before anything is written when it would be recorded for anyone.
        for (Person person : people.list(sheetId)) {
            if (selected.contains(String.valueOf(person.getId()))
                    && (!request.gradeFrom().isEmpty() || !request.grade().equals(person.getGrade()))) {
                grades.refuseRetired(sheetId, request.grade());
            }
        }
        for (Person person : people.list(sheetId)) {
            if (!selected.contains(String.valueOf(person.getId()))) {
                continue;
            }
            RollupFigures before = rollup.figures(sheetId);
            if (!request.gradeFrom().isEmpty() || !request.grade().equals(person.getGrade())) {
                LocalDate from = request.gradeFrom().isEmpty() ? person.getToday() : LocalDate.parse(request.gradeFrom());
                person.recordGrade(from, request.grade());
            }
            if (!people.save(person)) {
                throw new PersonConflictException(person.getId());
            }
            audit.recordOnCommit("PERSON_EDITED sheet=" + sheetId + " by=" + request.user()
                    + " name=" + person.getName() + " grade=" + person.getGrade());
            rollup.recordChanges(sheetId, request.user(), before);
            graded++;
        }
        response.send(new BulkGradeResponse(graded));
    }
}

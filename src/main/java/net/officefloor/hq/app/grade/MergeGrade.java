package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.person.People;
import net.officefloor.hq.app.person.Person;
import net.officefloor.hq.app.person.PersonConflictException;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupFigures;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * Gives every person whose grade in force on the sheet's today is the merged grade the other grade from
 * the sheet's today, in list order, each exactly as a person form save would: audited as PERSON_EDITED
 * with the rollup lines it moved; then, retroactively, every grade record of theirs on the merged grade becomes
 * the other grade; then the rollup lines the whole merge moved. Counted.
 */
public class MergeGrade {
    public void merge(@PathVariable("sheetId") String sheetId, @Val ValidGradeMerge request, People people,
            Rollup rollup, Audit audit, ObjectResponse<GradeMergeResponse> response) throws PersonConflictException {
        audit.recordOnCommit("GRADE_MERGED sheet=" + sheetId + " by=" + request.user()
                + " from=" + request.from() + " into=" + request.into());
        RollupFigures start = rollup.figures(sheetId);
        java.util.List<Person> moved = new java.util.ArrayList<>();
        for (Person person : people.list(sheetId)) {
            if (!request.from().equals(person.getGrade())) {
                continue;
            }
            RollupFigures before = rollup.figures(sheetId);
            person.recordGrade(person.getToday(), request.into());
            if (!people.save(person)) {
                throw new PersonConflictException(person.getId());
            }
            audit.recordOnCommit("PERSON_EDITED sheet=" + sheetId + " by=" + request.user()
                    + " name=" + person.getName() + " grade=" + person.getGrade());
            rollup.recordChanges(sheetId, request.user(), before);
            moved.add(person);
        }
        // The merge is retroactive: every grade record of each person it moved on the merged grade becomes the other.
        for (Person person : moved) {
            people.regrade(person, request.from(), request.into());
        }
        int merged = moved.size();
        if (merged > 0) {
            rollup.recordChanges(sheetId, request.user(), start);
        }
        response.send(new GradeMergeResponse(merged));
    }
}

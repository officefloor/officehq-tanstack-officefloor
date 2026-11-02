package net.officefloor.hq.app.person;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.grade.Grades;
import net.officefloor.hq.app.role.Role;
import net.officefloor.hq.app.role.Roles;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.sheet.SheetClock;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * A steward merges a grade into another: every person whose grade in force on the sheet's today is the merged grade is
 * given the other from the sheet's today, in list order (by position), exactly as the person form would, each with its
 * own PERSON_EDITED and rollup lines after the GRADE_MERGED line, then the rollup lines of the whole merge. Refused before anything is written for anyone but a
 * steward, a grade not the sheet's, the same grade twice, or a retired grade to give anyone.
 */
public class MergeGrade {

    public void merge(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody GradeMergeRequest request, People people, SheetClock clock, Audit audit, Rollup rollup,
            Roles roles, Grades grades, ObjectResponse<GradeMergeResponse> response)
            throws InvalidPersonException, DuplicatePersonNameException, StalePersonException {
        if (!Role.STEWARD.equals(roles.of(sheet, user))) {
            throw new InvalidPersonException("Only a steward may merge a grade");
        }
        String from = request == null || request.from() == null ? "" : request.from().trim();
        String into = request == null || request.into() == null ? "" : request.into().trim();
        if (!grades.isGrade(sheet, from) || !grades.isGrade(sheet, into)) {
            throw new InvalidPersonException("Grades must be grades of the sheet");
        }
        if (from.equals(into)) {
            throw new InvalidPersonException("A grade cannot be merged into itself");
        }
        List<Person> chosen = people.list(sheet).stream().filter((p) -> from.equals(p.grade())).toList();
        // Giving anyone a retired grade is refused, as the person form would: before anything is written.
        if (!chosen.isEmpty() && people.gradeRetired(sheet, into)) {
            throw new InvalidPersonException("Grade " + into + " is retired");
        }
        Rollup.Snapshot before = rollup.take(sheet);
        audit.record("GRADE_MERGED sheet=" + sheet + " by=" + user + " from=" + from + " into=" + into);
        for (Person person : chosen) {
            // Re-read so each is saved as the store holds it after the ones before it.
            Person current = people.find(sheet, person.id());
            SavePerson.apply(sheet, user, current,
                    new PersonRequest(current.name(), into, null, null,
                            current.terms() == null ? "" : current.terms().toString(),
                            current.contact() == null ? "" : current.contact(), null),
                    people, clock, audit, rollup);
        }
        // Then the figures the merge as a whole moved.
        rollup.record(sheet, user, before);
        response.send(new GradeMergeResponse(chosen.size(), people.list(sheet)));
    }
}

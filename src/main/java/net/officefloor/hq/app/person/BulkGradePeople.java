package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
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
 * Gives each selected person, in list order (by position), the grade from the from-date exactly as
 * the person form would, each with its own PERSON_EDITED and rollup lines. A bad grade or date, or a selected
 * person not on the sheet, refuses the whole action before anything is written. A viewer grades no one: it reports 0
 * and writes nothing.
 */
public class BulkGradePeople {
    public void grade(@PathVariable("sheet") String sheet, @RequestParam("user") String user,
            @RequestBody BulkGradeRequest request, People people, SheetClock clock, Audit audit, Rollup rollup, Roles roles,
            Grades grades, ObjectResponse<BulkGradeResponse> response)
            throws InvalidPersonException, DuplicatePersonNameException, StalePersonException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheet, grade)) {
            throw new InvalidPersonException("Grade must be one of the sheet's grades");
        }
        String from = request.from() == null ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                from = LocalDate.parse(from).toString();
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a valid from-date: " + from);
            }
        }
        Set<Long> selected = new HashSet<>(request.people() == null ? List.of() : request.people());
        List<Person> everyone = people.list(sheet);
        List<Person> chosen = everyone.stream().filter((p) -> selected.contains(p.id())).toList();
        if (chosen.size() != selected.size()) {
            throw new InvalidPersonException("Every selected person must be on the sheet");
        }
        // A retired grade may not be chosen for anyone: refused before anything is written.
        if (people.gradeRetired(sheet, grade)) {
            for (Person person : chosen) {
                if (!from.isEmpty() || !grade.equals(person.grade())) {
                    throw new InvalidPersonException("Grade " + grade + " is retired");
                }
            }
        }
        if (Role.VIEWER.equals(roles.of(sheet, user))) {
            response.send(new BulkGradeResponse(0, everyone));
            return;
        }
        for (Person person : chosen) {
            // Re-read so each is saved as the store holds it after the ones before it.
            Person current = people.find(sheet, person.id());
            SavePerson.apply(sheet, user, current,
                    new PersonRequest(current.name(), grade, null, from.isEmpty() ? null : from,
                            current.terms() == null ? "" : current.terms().toString(),
                            current.contact() == null ? "" : current.contact(), null),
                    people, clock, audit, rollup);
        }
        response.send(new BulkGradeResponse(chosen.size(), people.list(sheet)));
    }
}

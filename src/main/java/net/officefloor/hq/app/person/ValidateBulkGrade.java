package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a grade other than A, B or C, or a from-date that is not a date, before anything is read. */
public class ValidateBulkGrade {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody BulkGradeRequest request, Out<BulkGradeRequest> body)
            throws InvalidPersonException {
        if (request == null) throw new InvalidPersonException("A grade is one of A, B or C");
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) throw new InvalidPersonException("A grade is one of A, B or C");
        String gradeFrom = request.gradeFrom() == null ? "" : request.gradeFrom().trim();
        if (!gradeFrom.isEmpty()) {
            try {
                LocalDate.parse(gradeFrom);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("A from-date is a date, yyyy-MM-dd");
            }
        }
        List<String> ids = request.personIds() == null ? List.of() : request.personIds();
        body.set(new BulkGradeRequest(ids, grade, gradeFrom));
    }
}

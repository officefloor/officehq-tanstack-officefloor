package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a bulk grade with no one selected, a grade not A, B or C, or a from-date not a date. */
public class ValidateBulkGrade {
    private static final Set<String> GRADES = Set.of("A", "B", "C");

    public void validate(@RequestBody BulkGradeRequest request, Out<BulkGradeRequest> body)
            throws InvalidPersonException {
        List<Long> ids = request == null || request.personIds() == null ? List.of()
                : request.personIds().stream().filter((id) -> id != null).toList();
        if (ids.isEmpty()) {
            throw new InvalidPersonException("Select the people to grade");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GRADES.contains(grade)) {
            throw new InvalidPersonException("Grade must be A, B or C");
        }
        String from = request.from() == null ? "" : request.from().trim();
        if (!from.isEmpty()) {
            try {
                LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("Not a date: " + from);
            }
        }
        body.set(new BulkGradeRequest(ids, grade, from.isEmpty() ? null : from));
    }
}

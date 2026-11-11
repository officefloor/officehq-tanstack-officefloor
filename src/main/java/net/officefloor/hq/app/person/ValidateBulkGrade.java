package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a grade not shaped as one (one to three capital letters), or a from-date that is not a date, before anything is read. */
public class ValidateBulkGrade {

    public void validate(@RequestBody BulkGradeRequest request, Out<BulkGradeRequest> body)
            throws InvalidPersonException {
        if (request == null) throw new InvalidPersonException("A grade is one to three capital letters");
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!net.officefloor.hq.app.setting.Settings.isGradeCode(grade)) throw new InvalidPersonException("A grade is one to three capital letters");
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

package net.officefloor.hq.app.person;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.LinkedHashSet;
import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read: a signed-in user, at least one selected person, one of the
 *  sheet's grades, and a from-date (optional) that is a real yyyy-mm-dd date. */
public class ValidateBulkGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody BulkGradeRequest request,
            GradeRepository grades, Out<BulkGradeInput> body) throws InvalidPersonException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidPersonException("Sign in before changing people.");
        }
        if (request.personIds() == null || request.personIds().isEmpty()) {
            throw new InvalidPersonException("Select the people to grade.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheetId.trim(), grade)) {
            throw new InvalidPersonException("The grade must be one of the sheet's grades.");
        }
        String from = request.from() == null ? "" : request.from().trim();
        LocalDate fromDate = null;
        if (!from.isEmpty()) {
            try {
                fromDate = LocalDate.parse(from);
            } catch (DateTimeParseException e) {
                throw new InvalidPersonException("The grade's from-date must be a date, yyyy-mm-dd.");
            }
        }
        body.set(new BulkGradeInput(request.user().trim(), new LinkedHashSet<>(request.personIds()), grade, fromDate));
    }
}

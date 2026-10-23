package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and one of the sheet's grades. */
public class ValidateGradeRetire {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRetireRequest request,
            GradeRepository grades, Out<GradeRetireRequest> body)
            throws InvalidGradeException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidGradeException("Sign in before retiring a grade.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheetId.trim(), grade)) {
            throw new InvalidGradeException("The grade must be one of the sheet's grades.");
        }
        body.set(new GradeRetireRequest(request.user().trim(), grade));
    }
}

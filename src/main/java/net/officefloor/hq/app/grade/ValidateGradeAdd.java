package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a code (trimmed) of one to
 *  three capital letters. */
public class ValidateGradeAdd {
    public void validate(@RequestBody GradeAddRequest request, Out<GradeAddRequest> body) throws InvalidGradeException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidGradeException("Sign in before adding a grade.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!grade.matches("[A-Z]{1,3}")) {
            throw new InvalidGradeException("A grade is one to three capital letters.");
        }
        body.set(new GradeAddRequest(request.user().trim(), grade));
    }
}

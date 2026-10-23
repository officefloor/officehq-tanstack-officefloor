package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Bind the body and refuse it before anything is read or written: a signed-in user and a grade of A, B or C. */
public class ValidateGradeRetire {
    public void validate(@RequestBody GradeRetireRequest request, Out<GradeRetireRequest> body)
            throws InvalidGradeException {
        if (request == null || request.user() == null || request.user().isBlank()) {
            throw new InvalidGradeException("Sign in before retiring a grade.");
        }
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!GradeRepository.GRADES.contains(grade)) {
            throw new InvalidGradeException("The grade must be A, B or C.");
        }
        body.set(new GradeRetireRequest(request.user().trim(), grade));
    }
}

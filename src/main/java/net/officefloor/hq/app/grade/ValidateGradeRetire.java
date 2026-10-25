package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the grade to retire from the body, trimmed; refuses one other than A, B or C. */
public class ValidateGradeRetire {
    public void validate(@RequestBody GradeRetireRequest request, Out<GradeRetireRequest> body) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!RetiredGrades.GRADES.contains(grade)) {
            throw new InvalidGradeException("The grade must be A, B or C.");
        }
        body.set(new GradeRetireRequest(grade));
    }
}

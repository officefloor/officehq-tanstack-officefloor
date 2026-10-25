package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the grade to retire from the body, trimmed; refuses one not shaped as a grade. */
public class ValidateGradeRetire {
    public void validate(@RequestBody GradeRetireRequest request, Out<GradeRetireRequest> body) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!RetiredGrades.isCode(grade)) {
            throw new InvalidGradeException("The grade must be one of the sheet's grades.");
        }
        body.set(new GradeRetireRequest(grade));
    }
}

package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the grade to add from the body, trimmed; refuses one that is not a code of one to three capital letters. */
public class ValidateGradeAdd {
    public void validate(@RequestBody GradeAddRequest request, Out<GradeAddRequest> body) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!RetiredGrades.isCode(grade)) {
            throw new InvalidGradeException("A grade must be one to three capital letters.");
        }
        body.set(new GradeAddRequest(grade));
    }
}

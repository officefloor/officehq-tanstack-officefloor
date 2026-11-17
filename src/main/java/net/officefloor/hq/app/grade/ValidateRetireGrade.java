package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the grade is A, B or C. */
public class ValidateRetireGrade {
    public void validate(@RequestBody RetireGradeRequest request, Out<RetireGradeValues> values) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GradeRepository.GRADES.contains(grade)) {
            throw new InvalidGradeException("Grade must be A, B or C");
        }
        values.set(new RetireGradeValues(grade, request.by() == null ? "" : request.by().trim()));
    }
}

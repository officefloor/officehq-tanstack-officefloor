package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the code (kept without the spaces around it) is one to three capital letters. */
public class ValidateAddGrade {
    public void validate(@RequestBody AddGradeRequest request, Out<AddGradeValues> values) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!grade.matches("[A-Z]{1,3}")) {
            throw new InvalidGradeException("A grade is one to three capital letters");
        }
        values.set(new AddGradeValues(grade, request.by() == null ? "" : request.by().trim()));
    }
}

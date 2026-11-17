package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Reads the body: the grade is one of the sheet's grades. */
public class ValidateRetireGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody RetireGradeRequest request, GradeRepository grades,
            Out<RetireGradeValues> values) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheetId.trim(), grade)) {
            throw new InvalidGradeException("Grade must be one of the sheet's grades");
        }
        values.set(new RetireGradeValues(grade, request.by() == null ? "" : request.by().trim()));
    }
}

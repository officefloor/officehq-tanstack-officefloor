package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a grade not a grade of the sheet, and for a grade already retired. */
public class ValidateRetireGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRequest request,
            GradeRepository grades, Out<GradeRequest> body) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheetId.trim(), grade)) {
            throw new InvalidGradeException("Not a grade of the sheet");
        }
        if (grades.isRetired(sheetId.trim(), grade)) {
            throw new InvalidGradeException("The grade is already retired: " + grade);
        }
        body.set(new GradeRequest(grade));
    }
}

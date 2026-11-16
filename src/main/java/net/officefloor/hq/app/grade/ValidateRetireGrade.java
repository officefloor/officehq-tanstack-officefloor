package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a grade not A, B or C, and for a grade already retired. */
public class ValidateRetireGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRequest request,
            GradeRepository grades, Out<GradeRequest> body) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GradeRepository.GRADES.contains(grade)) {
            throw new InvalidGradeException("Grade must be A, B or C");
        }
        if (grades.isRetired(sheetId.trim(), grade)) {
            throw new InvalidGradeException("The grade is already retired: " + grade);
        }
        body.set(new GradeRequest(grade));
    }
}

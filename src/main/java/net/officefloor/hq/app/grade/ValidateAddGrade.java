package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refused for a code not one to three capital letters, and for one already a grade of the sheet. */
public class ValidateAddGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRequest request,
            GradeRepository grades, Out<GradeRequest> body) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GradeRepository.CODE.matcher(grade).matches()) {
            throw new InvalidGradeException("A grade is one to three capital letters");
        }
        if (grades.isGrade(sheetId.trim(), grade)) {
            throw new InvalidGradeException("That is already a grade: " + grade);
        }
        body.set(new GradeRequest(grade));
    }
}

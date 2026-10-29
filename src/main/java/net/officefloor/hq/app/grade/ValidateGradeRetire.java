package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGradeRetire {

    // The grade, trimmed, must be A, B or C and not already retired on the sheet.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRequest body, Grades grades,
            Out<RetiredGrade> out) throws GradeRefusedException {
        String sheet = sheetId.trim();
        String grade = body == null || body.grade() == null ? "" : body.grade().trim();
        if (!Grades.ALL.contains(grade)) {
            throw new GradeRefusedException("The grade must be A, B or C.");
        }
        if (grades.retired(sheet, grade)) {
            throw new GradeRefusedException("Grade " + grade + " is already retired.");
        }
        out.set(new RetiredGrade(sheet, grade));
    }
}

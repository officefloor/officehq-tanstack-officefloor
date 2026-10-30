package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGradeAdd {

    // The grade, trimmed, must be a code of one to three capital letters, not yet a grade on the sheet.
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRequest body, Grades grades,
            Out<AddedGrade> out) throws GradeRefusedException {
        String sheet = sheetId.trim();
        String grade = body == null || body.grade() == null ? "" : body.grade().trim();
        if (!Grades.CODE.matcher(grade).matches()) {
            throw new GradeRefusedException("A grade must be one to three capital letters.");
        }
        if (grades.exists(sheet, grade)) {
            throw new GradeRefusedException("Grade " + grade + " is already a grade.");
        }
        out.set(new AddedGrade(sheet, grade));
    }
}

package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGradeAdd {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeAddRequest request,
            Out<ValidGradeAdd> body, Settings settings, RetiredGrades grades)
            throws InvalidGradeException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!RetiredGrades.CODE.matcher(grade).matches()) {
            throw new InvalidGradeException("A grade must be one to three capital letters");
        }
        if (grades.isGrade(sheetId, grade)) {
            throw new InvalidGradeException("Grade " + grade + " is already a grade");
        }
        body.set(new ValidGradeAdd(grade, request.user()));
    }
}

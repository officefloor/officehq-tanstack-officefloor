package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.setting.Settings;
import net.officefloor.hq.app.setting.ViewerRefusedException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

public class ValidateGradeRetire {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRetireRequest request,
            Out<ValidGradeRetire> body, Settings settings, RetiredGrades grades)
            throws InvalidGradeException, ViewerRefusedException {
        settings.refuseViewer(sheetId, request.user());
        String grade = request.grade() == null ? "" : request.grade().trim();
        if (!grades.isGrade(sheetId, grade)) {
            throw new InvalidGradeException("Grade must be one of the sheet's grades");
        }
        if (grades.isRetired(sheetId, grade)) {
            throw new InvalidGradeException("Grade " + grade + " is already retired");
        }
        body.set(new ValidGradeRetire(grade, request.user()));
    }
}

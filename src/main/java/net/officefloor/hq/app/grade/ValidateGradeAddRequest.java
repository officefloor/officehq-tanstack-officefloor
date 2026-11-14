package net.officefloor.hq.app.grade;

import jakarta.validation.Valid;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a code that is not one to three capital letters, or one already a grade on the sheet. */
public class ValidateGradeAddRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid GradeAddRequest request, GradeRepository grades,
            Out<GradeAdded> added) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!GradeRepository.CODE.matcher(grade).matches()) {
            throw new InvalidGradeException("A grade is one to three capital letters.");
        }
        if (grades.exists(sheet.id(), grade)) {
            throw new InvalidGradeException("The grade " + grade + " is already a grade.");
        }
        added.set(new GradeAdded(sheet.id(), grade));
    }
}

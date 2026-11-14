package net.officefloor.hq.app.grade;

import jakarta.validation.Valid;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses (400) a grade that is not one of the sheet's, or one already retired on the sheet. */
public class ValidateGradeRetireRequest {
    public void validate(@Val Sheet sheet, @RequestBody @Valid GradeRetireRequest request, GradeRepository grades,
            Out<GradeRetired> retired) throws InvalidGradeException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim().toUpperCase();
        if (!grades.exists(sheet.id(), grade)) {
            throw new InvalidGradeException("The grade must be one of the sheet's grades.");
        }
        if (grades.isRetired(sheet.id(), grade)) {
            throw new InvalidGradeException("The grade " + grade + " is already retired.");
        }
        retired.set(new GradeRetired(sheet.id(), grade));
    }
}

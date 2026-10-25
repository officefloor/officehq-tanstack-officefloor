package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Retires the grade on the sheet; refuses one already retired. */
public class ApplyGradeRetire {
    public void apply(@Val GradeRetireRequest request, @Val Sheet sheet, RetiredGradeRepository repository,
            Out<RetiredGrade> retiredOut) throws InvalidGradeException {
        if (repository.existsBySheetIdAndGrade(sheet.getId(), request.grade())) {
            throw new InvalidGradeException("The grade " + request.grade() + " is already retired.");
        }
        retiredOut.set(repository.saveAndFlush(new RetiredGrade(sheet.getId(), request.grade())));
    }
}

package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A grade already retired on the sheet is refused. */
public class RetireGrade {
    public void retire(@Val GradeRequest request, @Val Sheet sheet, GradeRepository repository,
            Out<RetiredGrade> retiredOut) throws InvalidGradeException {
        if (repository.isRetired(sheet.id(), request.grade())) {
            throw new InvalidGradeException("Grade " + request.grade() + " is already retired");
        }
        repository.retire(sheet.id(), request.grade());
        retiredOut.set(new RetiredGrade(sheet.id(), request.grade()));
    }
}

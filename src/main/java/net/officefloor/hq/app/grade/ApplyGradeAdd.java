package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** Adds the grade to the sheet; refuses one that is already a grade on it. */
public class ApplyGradeAdd {
    public void apply(@Val GradeAddRequest request, @Val Sheet sheet, AddedGradeRepository repository,
            Out<AddedGrade> addedOut) throws InvalidGradeException {
        if (RetiredGrades.isGrade(sheet.getId(), request.grade(), repository)) {
            throw new InvalidGradeException("The grade " + request.grade() + " is already a grade.");
        }
        addedOut.set(repository.saveAndFlush(new AddedGrade(sheet.getId(), request.grade())));
    }
}

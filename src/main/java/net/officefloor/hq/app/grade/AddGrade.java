package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Out;
import net.officefloor.plugin.variable.Val;

/** A grade already on the sheet is refused. */
public class AddGrade {
    public void add(@Val GradeRequest request, @Val Sheet sheet, GradeRepository repository,
            Out<AddedGrade> addedOut) throws InvalidGradeException {
        if (repository.exists(sheet.id(), request.grade())) {
            throw new InvalidGradeException("Grade " + request.grade() + " is already a grade");
        }
        repository.add(sheet.id(), request.grade());
        addedOut.set(new AddedGrade(sheet.id(), request.grade()));
    }
}

package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.sheet.InvalidSheetRequestException;
import net.officefloor.plugin.variable.Out;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;

/** Refuses a code that is not one to three capital letters (the spaces around it aside), or that is
 *  already one of the sheet's grades. */
public class ValidateAddGrade {
    public void validate(@PathVariable("sheetId") String sheetId, @RequestBody GradeRequest request, Grades grades,
            Out<GradeAddition> addition) throws InvalidSheetRequestException {
        String grade = request == null || request.grade() == null ? "" : request.grade().trim();
        if (!Grades.CODE.matcher(grade).matches()) {
            throw new InvalidSheetRequestException("A grade is one to three capital letters");
        }
        if (grades.isGrade(sheetId, grade)) {
            throw new InvalidSheetRequestException("The grade " + grade + " is already a grade");
        }
        addition.set(new GradeAddition(sheetId, grade, request.user()));
    }
}

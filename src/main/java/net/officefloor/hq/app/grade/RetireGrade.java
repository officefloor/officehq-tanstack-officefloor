package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Retires the grade on the sheet; one already retired is refused. */
public class RetireGrade {
    public void retire(@PathVariable("sheetId") String sheetId, @Val RetireGradeValues values, GradeRepository grades)
            throws InvalidGradeException {
        String sheet = sheetId.trim();
        if (grades.isRetired(sheet, values.grade())) {
            throw new InvalidGradeException("Grade " + values.grade() + " is already retired");
        }
        grades.retire(sheet, values.grade());
    }
}

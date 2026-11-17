package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the grade to the sheet and writes GRADE_ADDED; one already a grade of the sheet is refused. */
public class AddGrade {
    public void add(@PathVariable("sheetId") String sheetId, @Val AddGradeValues values, GradeRepository grades, Audit audit)
            throws InvalidGradeException {
        String sheet = sheetId.trim();
        if (grades.isGrade(sheet, values.grade())) {
            throw new InvalidGradeException("Grade " + values.grade() + " is already a grade");
        }
        grades.add(sheet, values.grade());
        audit.record("GRADE_ADDED sheet=" + sheet + " by=" + values.by() + " grade=" + values.grade());
    }
}

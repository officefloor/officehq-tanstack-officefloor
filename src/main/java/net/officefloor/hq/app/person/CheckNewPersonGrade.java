package net.officefloor.hq.app.person;

import net.officefloor.hq.app.grade.GradeRepository;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuses a new person on a grade retired on the sheet (see GradeRepository). */
public class CheckNewPersonGrade {
    public void check(@PathVariable("sheetId") String sheetId, @Val PersonValues values, GradeRepository grades)
            throws InvalidPersonException {
        grades.checkChoosable(sheetId.trim(), values.grade());
    }
}

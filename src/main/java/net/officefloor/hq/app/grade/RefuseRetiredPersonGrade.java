package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.person.PersonRequest;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse a person form's add or edit whose grade is retired on the sheet. */
public class RefuseRetiredPersonGrade {
    public void refuse(@PathVariable("sheetId") String sheetId, @Val PersonRequest request, GradeRepository grades)
            throws RetiredGradeException {
        grades.refuseRetired(sheetId.trim(), request.grade());
    }
}

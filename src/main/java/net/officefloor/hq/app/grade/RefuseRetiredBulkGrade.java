package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.person.BulkGradeInput;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse a bulk grade to a grade retired on the sheet. */
public class RefuseRetiredBulkGrade {
    public void refuse(@PathVariable("sheetId") String sheetId, @Val BulkGradeInput input, GradeRepository grades)
            throws RetiredGradeException {
        grades.refuseRetired(sheetId.trim(), input.grade());
    }
}

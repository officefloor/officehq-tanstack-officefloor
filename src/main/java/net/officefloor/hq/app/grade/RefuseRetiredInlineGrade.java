package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.person.InlineGradeRequest;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Refuse a grade chosen on a person's row that is retired on the sheet. */
public class RefuseRetiredInlineGrade {
    public void refuse(@PathVariable("sheetId") String sheetId, @Val InlineGradeRequest request, GradeRepository grades)
            throws RetiredGradeException {
        grades.refuseRetired(sheetId.trim(), request.grade());
    }
}

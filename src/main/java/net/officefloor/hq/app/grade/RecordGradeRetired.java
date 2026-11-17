package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Writes GRADE_RETIRED to the audit file once the grade is retired. */
public class RecordGradeRetired {
    public void record(@PathVariable("sheetId") String sheetId, @Val RetireGradeValues values, Audit audit) {
        audit.record("GRADE_RETIRED sheet=" + sheetId.trim() + " by=" + values.by() + " grade=" + values.grade());
    }
}

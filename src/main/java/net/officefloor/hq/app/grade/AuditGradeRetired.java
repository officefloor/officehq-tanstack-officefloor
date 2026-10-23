package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Record the retired grade in the audit file. */
public class AuditGradeRetired {
    public void audit(@PathVariable("sheetId") String sheetId, @Val GradeRetireRequest request, Audit audit) {
        audit.record("GRADE_RETIRED sheet=" + sheetId.trim() + " by=" + request.user() + " grade=" + request.grade());
    }
}

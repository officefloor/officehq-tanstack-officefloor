package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Record the added grade in the audit file. */
public class AuditGradeAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val GradeAddRequest request, Audit audit) {
        audit.record("GRADE_ADDED sheet=" + sheetId.trim() + " by=" + request.user() + " grade=" + request.grade());
    }
}

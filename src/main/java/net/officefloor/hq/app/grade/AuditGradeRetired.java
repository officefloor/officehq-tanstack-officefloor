package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the grade retired on the sheet. */
public class AuditGradeRetired {
    public void audit(@Val GradeRetired retired, @RequestParam("user") String user, Audit audit) {
        audit.record("GRADE_RETIRED sheet=" + retired.sheetId() + " by=" + user + " grade=" + retired.grade());
    }
}

package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the grade, once it is retired. */
public class AuditGradeRetired {
    public void audit(@Val RetiredGrade retired, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("GRADE_RETIRED sheet=" + retired.getSheetId() + " by=" + AuditUser.decode(user)
                + " grade=" + retired.getGrade());
    }
}

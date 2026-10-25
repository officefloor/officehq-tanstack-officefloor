package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.AuditUser;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestHeader;

/** Records the grade, once it is added. */
public class AuditGradeAdded {
    public void audit(@Val AddedGrade added, @RequestHeader(name = "X-User", required = false) String user, Audit audit) {
        audit.record("GRADE_ADDED sheet=" + added.getSheetId() + " by=" + AuditUser.decode(user)
                + " grade=" + added.getGrade());
    }
}

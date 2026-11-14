package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the grade added on the sheet. */
public class AuditGradeAdded {
    public void audit(@Val GradeAdded added, @RequestParam("user") String user, Audit audit) {
        audit.record("GRADE_ADDED sheet=" + added.sheetId() + " by=" + user + " grade=" + added.grade());
    }
}

package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a grade added: GRADE_ADDED sheet=<id> by=<user> grade=<grade>. */
public class AuditGradeAdded {
    public void audit(@PathVariable("sheetId") String sheetId, @Val GradeAddInput input, Audit audit) {
        audit.record("GRADE_ADDED sheet=" + sheetId.trim() + " by=" + input.user() + " grade=" + input.grade());
    }
}

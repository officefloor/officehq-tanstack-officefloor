package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.PathVariable;

/** Records a grade retired: GRADE_RETIRED sheet=<id> by=<user> grade=<grade>. */
public class AuditGradeRetired {
    public void audit(@PathVariable("sheetId") String sheetId, @Val GradeRetireInput input, Audit audit) {
        audit.record("GRADE_RETIRED sheet=" + sheetId.trim() + " by=" + input.user() + " grade=" + input.grade());
    }
}

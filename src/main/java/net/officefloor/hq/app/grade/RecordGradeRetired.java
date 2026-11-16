package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a retirement that went through is recorded. */
public class RecordGradeRetired {
    public void record(@Val GradeRetired retired, Audit audit) {
        audit.record("GRADE_RETIRED sheet=" + retired.sheetId() + " by=" + retired.by() + " grade=" + retired.grade());
    }
}

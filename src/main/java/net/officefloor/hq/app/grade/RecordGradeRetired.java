package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the retirement is saved, so a refused one writes no audit line. */
public class RecordGradeRetired {
    public void record(@Val GradeRetirement retirement, Audit audit) {
        audit.record("GRADE_RETIRED sheet=" + retirement.sheetId() + " by=" + retirement.by()
                + " grade=" + retirement.grade());
    }
}

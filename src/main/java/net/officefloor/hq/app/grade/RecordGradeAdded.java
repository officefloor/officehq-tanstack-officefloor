package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Last write step: only a grade added that went through is recorded. */
public class RecordGradeAdded {
    public void record(@Val GradeAdded added, Audit audit) {
        audit.record("GRADE_ADDED sheet=" + added.sheetId() + " by=" + added.by() + " grade=" + added.grade());
    }
}

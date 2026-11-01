package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Runs after the grade is saved, so a refused one writes no audit line. */
public class RecordGradeAdded {
    public void record(@Val GradeAddition addition, Audit audit) {
        audit.record("GRADE_ADDED sheet=" + addition.sheetId() + " by=" + addition.by() + " grade=" + addition.grade());
    }
}

package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;

public class SaveGradeRetired {
    public void save(@Val GradeRetirement retirement, Grades grades) {
        grades.retire(retirement.sheetId(), retirement.grade());
    }
}

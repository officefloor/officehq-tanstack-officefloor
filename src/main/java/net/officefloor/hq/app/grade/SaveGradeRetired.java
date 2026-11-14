package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;

public class SaveGradeRetired {
    public void save(@Val GradeRetired retired, GradeRepository grades) {
        grades.retire(retired.sheetId(), retired.grade());
    }
}

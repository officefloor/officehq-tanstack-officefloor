package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;

public class SaveGradeAdded {
    public void save(@Val GradeAdded added, GradeRepository grades) {
        grades.add(added.sheetId(), added.grade());
    }
}

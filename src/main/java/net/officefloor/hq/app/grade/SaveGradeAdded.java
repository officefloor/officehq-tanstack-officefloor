package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;

public class SaveGradeAdded {
    public void save(@Val GradeAddition addition, Grades grades) {
        grades.add(addition.sheetId(), addition.grade());
    }
}

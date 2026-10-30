package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;

public class AddGrade {
    public void add(@Val AddedGrade grade, Grades grades) {
        grades.add(grade.sheetId(), grade.grade());
    }
}

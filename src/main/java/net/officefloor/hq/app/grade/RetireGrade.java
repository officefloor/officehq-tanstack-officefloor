package net.officefloor.hq.app.grade;

import net.officefloor.plugin.variable.Val;

public class RetireGrade {
    public void retire(@Val RetiredGrade grade, Grades grades) {
        grades.retire(grade.sheetId(), grade.grade());
    }
}

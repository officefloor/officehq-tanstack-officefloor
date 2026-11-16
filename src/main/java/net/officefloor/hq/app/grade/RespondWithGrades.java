package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGrades {
    public void respond(@Val GradeRetired retired, GradeRepository grades, ObjectResponse<List<Grade>> response) {
        response.send(grades.list(retired.sheetId()));
    }
}

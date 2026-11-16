package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

public class RespondWithGradesAdded {
    public void respond(@Val GradeAdded added, GradeRepository grades, ObjectResponse<List<Grade>> response) {
        response.send(grades.list(added.sheetId()));
    }
}

package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's grades, A to C then those added, each with whether it is retired. */
public class RespondWithGrades {
    public void respond(@Val Sheet sheet, GradeRepository grades, ObjectResponse<List<GradeResponse>> response) {
        Set<String> retired = grades.retired(sheet.id());
        response.send(grades.grades(sheet.id()).stream().map((g) -> new GradeResponse(g, retired.contains(g))).toList());
    }
}

package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The grades A, B and C, each with whether it is retired on the sheet. */
public class RespondWithGrades {
    public void respond(@Val Sheet sheet, RetiredGradeRepository repository, ObjectResponse<List<GradeResponse>> response) {
        Set<String> retired = RetiredGrades.of(sheet.getId(), repository);
        response.send(RetiredGrades.GRADES.stream().map(g -> new GradeResponse(g, retired.contains(g))).toList());
    }
}

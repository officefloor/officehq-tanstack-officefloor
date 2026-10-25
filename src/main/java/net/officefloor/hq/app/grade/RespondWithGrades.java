package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;

/** The sheet's grades (A, B and C, then those added in the order added), each with whether it is retired on the sheet. */
public class RespondWithGrades {
    public void respond(@Val Sheet sheet, RetiredGradeRepository repository, AddedGradeRepository added,
            ObjectResponse<List<GradeResponse>> response) {
        Set<String> retired = RetiredGrades.of(sheet.getId(), repository);
        response.send(RetiredGrades.all(sheet.getId(), added).stream()
                .map(g -> new GradeResponse(g, retired.contains(g))).toList());
    }
}

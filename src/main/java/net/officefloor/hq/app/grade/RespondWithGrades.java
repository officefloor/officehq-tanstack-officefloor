package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's grades (see GradeRepository#grades), each with whether it is retired on the sheet. */
public class RespondWithGrades {
    public void respond(@PathVariable("sheetId") String sheetId, GradeRepository grades,
            ObjectResponse<List<GradeResponse>> response) {
        Set<String> retired = grades.retired(sheetId.trim());
        response.send(grades.grades(sheetId.trim()).stream().map((g) -> new GradeResponse(g, retired.contains(g))).toList());
    }
}

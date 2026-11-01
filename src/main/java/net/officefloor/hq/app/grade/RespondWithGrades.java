package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Every grade of the sheet, in order, each with whether it is retired. */
public class RespondWithGrades {
    public void respond(@PathVariable("sheetId") String sheetId, Grades grades,
            ObjectResponse<List<GradeResponse>> response) {
        Set<String> retired = grades.retired(sheetId);
        response.send(grades.all(sheetId).stream().map(g -> new GradeResponse(g, retired.contains(g))).toList());
    }
}

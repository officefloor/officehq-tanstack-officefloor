package net.officefloor.hq.app.grade;

import java.util.List;
import java.util.Set;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The grades A, B and C, each with whether it is retired on the sheet. */
public class RespondWithGrades {
    public void respond(@PathVariable("sheetId") String sheetId, GradeRepository grades,
            ObjectResponse<List<GradeResponse>> response) {
        Set<String> retired = grades.retired(sheetId.trim());
        response.send(GradeRepository.GRADES.stream().map((g) -> new GradeResponse(g, retired.contains(g))).toList());
    }
}

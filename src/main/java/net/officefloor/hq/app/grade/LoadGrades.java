package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** The sheet's grades, each with whether it is retired. */
public class LoadGrades {
    public void service(@PathVariable("sheetId") String sheetId, RetiredGrades grades, ObjectResponse<List<GradeResponse>> response) {
        response.send(grades.list(sheetId));
    }
}

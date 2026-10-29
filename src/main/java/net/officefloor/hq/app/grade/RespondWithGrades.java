package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class RespondWithGrades {
    public void respond(@PathVariable("sheetId") String sheetId, Grades grades, ObjectResponse<List<GradeResponse>> response) {
        response.send(grades.list(sheetId.trim()));
    }
}

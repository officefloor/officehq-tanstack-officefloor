package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

public class ListGrades {
    public void list(@PathVariable("sheetId") String sheetId, GradeRepository grades,
            ObjectResponse<List<Grade>> response) {
        response.send(grades.list(sheetId.trim()));
    }
}

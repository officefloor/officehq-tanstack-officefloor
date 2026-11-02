package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Responds with the sheet's grades and whether each is retired. */
public class RespondWithGrades {
    public void respond(@PathVariable("sheet") String sheet, Grades grades, ObjectResponse<List<GradeRow>> response) {
        response.send(grades.list(sheet));
    }
}

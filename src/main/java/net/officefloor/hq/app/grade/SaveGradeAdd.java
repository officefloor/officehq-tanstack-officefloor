package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Adds the grade to the sheet; it can be chosen from then on. */
public class SaveGradeAdd {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidGradeAdd request, RetiredGrades grades, Audit audit,
            ObjectResponse<List<GradeResponse>> response) {
        grades.add(sheetId, request.grade());
        audit.recordOnCommit("GRADE_ADDED sheet=" + sheetId + " by=" + request.user() + " grade=" + request.grade());
        response.send(grades.list(sheetId));
    }
}

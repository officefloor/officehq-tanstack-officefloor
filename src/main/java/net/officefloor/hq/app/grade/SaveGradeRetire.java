package net.officefloor.hq.app.grade;

import java.util.List;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.web.ObjectResponse;
import org.springframework.web.bind.annotation.PathVariable;

/** Retires the grade on the sheet; people already on it keep it. */
public class SaveGradeRetire {
    public void save(@PathVariable("sheetId") String sheetId, @Val ValidGradeRetire request, RetiredGrades grades, Audit audit,
            ObjectResponse<List<GradeResponse>> response) {
        grades.retire(sheetId, request.grade());
        audit.recordOnCommit("GRADE_RETIRED sheet=" + sheetId + " by=" + request.user() + " grade=" + request.grade());
        response.send(grades.list(sheetId));
    }
}

package net.officefloor.hq.app.grade;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the grade retired. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditGradeRetired {
    public void audit(@Val RetiredGrade retired, HttpServletRequest request, Audit audit) {
        audit.record("GRADE_RETIRED sheet=" + retired.sheetId() + " by=" + Audit.user(request)
                + " grade=" + retired.grade());
    }
}

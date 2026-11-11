package net.officefloor.hq.app.grade;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** Records the grade added. Runs after every rule has passed, so a refused change writes nothing. */
public class AuditGradeAdded {
    public void audit(@Val AddedGrade added, HttpServletRequest request, Audit audit) {
        audit.record("GRADE_ADDED sheet=" + added.sheetId() + " by=" + Audit.user(request)
                + " grade=" + added.grade());
    }
}

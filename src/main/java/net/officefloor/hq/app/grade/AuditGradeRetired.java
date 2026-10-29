package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditGradeRetired {
    public void audit(@Val RetiredGrade grade, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("GRADE_RETIRED", grade.sheetId(), Audit.user(connection), "grade=" + grade.grade()));
    }
}

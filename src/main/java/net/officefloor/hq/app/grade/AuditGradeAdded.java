package net.officefloor.hq.app.grade;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import net.officefloor.server.http.ServerHttpConnection;

public class AuditGradeAdded {
    public void audit(@Val AddedGrade grade, ServerHttpConnection connection, Audit audit) {
        audit.record(Audit.line("GRADE_ADDED", grade.sheetId(), Audit.user(connection), "grade=" + grade.grade()));
    }
}

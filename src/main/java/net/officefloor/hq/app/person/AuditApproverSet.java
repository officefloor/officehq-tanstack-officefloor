package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the approver. Runs after it is saved. */
public class AuditApproverSet {
    public void audit(@Val Sheet sheet, @Val Person person, HttpServletRequest request, Audit audit) {
        audit.record("APPROVER_SET sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + person.getName() + " user=" + (person.getApprover() == null ? "" : person.getApprover()));
    }
}

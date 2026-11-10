package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records the transfer on this sheet. Runs after it is saved. */
public class AuditPersonTransferred {
    public void audit(@Val Sheet sheet, @Val Transfer transfer, HttpServletRequest request, Audit audit) {
        audit.record("PERSON_TRANSFERRED sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + transfer.person().getName() + " to=" + transfer.to().id());
    }
}

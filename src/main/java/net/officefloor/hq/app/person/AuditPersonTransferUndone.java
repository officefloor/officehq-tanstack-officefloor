package net.officefloor.hq.app.person;

import jakarta.servlet.http.HttpServletRequest;
import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.sheet.Sheet;
import net.officefloor.plugin.variable.Val;

/** Records on this sheet that the transfer that brought the person here was undone. Runs after it is saved. */
public class AuditPersonTransferUndone {
    public void audit(@Val Sheet sheet, @Val Transfer transfer, HttpServletRequest request, Audit audit) {
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + sheet.id() + " by=" + Audit.user(request)
                + " name=" + transfer.person().getName() + " to=" + transfer.to().id());
    }
}

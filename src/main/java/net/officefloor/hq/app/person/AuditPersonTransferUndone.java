package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records, on this sheet, the person whose transfer here was undone and the sheet they went back to. */
public class AuditPersonTransferUndone {
    public void audit(@Val Transfer transfer, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + transfer.person().sheetId() + " by=" + user + " name="
                + transfer.person().name() + " to=" + transfer.to());
    }
}

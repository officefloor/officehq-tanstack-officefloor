package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records, on this sheet, the person just transferred and the sheet they went to. */
public class AuditPersonTransferred {
    public void audit(@Val Transfer transfer, @RequestParam("user") String user, Audit audit) {
        audit.record("PERSON_TRANSFERRED sheet=" + transfer.person().sheetId() + " by=" + user + " name="
                + transfer.person().name() + " to=" + transfer.to());
    }
}

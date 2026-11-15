package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;
import org.springframework.web.bind.annotation.RequestParam;

/** Records the transfer just undone on the sheet the person left (PERSON_TRANSFER_UNDONE ... to=). */
public class RecordPersonTransferUndone {
    public void record(@RequestParam(name = "by", required = false) String by,
            @Val Transfer transfer, Audit audit) {
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + transfer.person().getSheetId() + " by=" + (by == null ? "" : by)
                + " name=" + transfer.person().getName() + " to=" + transfer.to());
    }
}

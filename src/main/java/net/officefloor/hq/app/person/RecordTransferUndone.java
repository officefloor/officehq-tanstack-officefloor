package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.plugin.variable.Val;

/** After the transfer back is saved and audited: this sheet's line that the transfer was undone. */
public class RecordTransferUndone {
    public void record(@Val Transfer transfer, Audit audit) {
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + transfer.from() + " by=" + transfer.user()
                + " name=" + transfer.name() + " to=" + transfer.to());
    }
}

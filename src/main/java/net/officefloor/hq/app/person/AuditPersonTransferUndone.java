package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.Rollups;
import net.officefloor.plugin.variable.Val;

/** Record on this sheet that the transfer was undone (after the transfer back's own records), then how its figures
 *  moved. */
public class AuditPersonTransferUndone {
    public void audit(@Val TransferInput input, @Val Transfer transfer, @Val Rollup before, Audit audit,
            Rollups rollups) {
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + transfer.fromSheet() + " by=" + input.user() + " name="
                + transfer.name() + " to=" + transfer.toSheet());
        rollups.record(transfer.fromSheet(), input.user(), before);
    }
}

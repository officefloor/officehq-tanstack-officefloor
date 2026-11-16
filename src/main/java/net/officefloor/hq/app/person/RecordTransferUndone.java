package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** After the transfer back's own lines (see RecordPersonTransferred), writes PERSON_TRANSFER_UNDONE name=<name>
 *  to=<sheet> on this sheet, then this sheet's figures the undo moved. */
public class RecordTransferUndone {
    public void record(@Val PersonTransfer transfer, @Val RollupBefore before, Rollup rollup, Audit audit) {
        String sheet = transfer.person().sheetId();
        audit.record("PERSON_TRANSFER_UNDONE sheet=" + sheet + " by=" + transfer.by() + " name=" + transfer.person().name()
                + " to=" + transfer.toSheet());
        rollup.record(sheet, transfer.by(), before);
    }
}

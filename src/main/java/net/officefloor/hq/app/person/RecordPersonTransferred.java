package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_TRANSFERRED name=<name> to=<sheet> on the sheet the person left, then that sheet's figures the transfer
 *  moved (see Rollup), then PERSON_TRANSFERRED name=<name> from=<sheet> on the sheet they went to, then that sheet's. */
public class RecordPersonTransferred {
    public void record(@Val PersonTransfer transfer, @Val RollupBefore before, Rollup rollup, Audit audit) {
        String from = transfer.person().sheetId();
        String to = transfer.toSheet();
        String name = transfer.person().name();
        audit.record("PERSON_TRANSFERRED sheet=" + from + " by=" + transfer.by() + " name=" + name + " to=" + to);
        rollup.record(from, transfer.by(), before);
        audit.record("PERSON_TRANSFERRED sheet=" + to + " by=" + transfer.by() + " name=" + name + " from=" + from);
        rollup.record(to, transfer.by(), transfer.toBefore());
    }
}

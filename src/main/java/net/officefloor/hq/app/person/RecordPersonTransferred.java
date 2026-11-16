package net.officefloor.hq.app.person;

import net.officefloor.hq.app.Audit;
import net.officefloor.hq.app.rollup.Rollup;
import net.officefloor.hq.app.rollup.RollupBefore;
import net.officefloor.plugin.variable.Val;

/** Writes PERSON_TRANSFERRED name=<name> to=<sheet> on the sheet the person left, then that sheet's figures the transfer
 *  moved (see Rollup), then PERSON_TRANSFERRED name=<name> from=<sheet> on the sheet they went to (followed by
 *  PERSON_MERGED from=<name> into=<name> when they merged into someone there), then that sheet's. */
public class RecordPersonTransferred {
    public void record(@Val PersonTransfer transfer, @Val RollupBefore before, Rollup rollup, Audit audit) {
        String from = transfer.person().sheetId();
        String to = transfer.toSheet();
        String name = transfer.person().name();
        audit.record("PERSON_TRANSFERRED sheet=" + from + " by=" + transfer.by() + " name=" + name + " to=" + to);
        rollup.record(from, transfer.by(), before);
        audit.record("PERSON_TRANSFERRED sheet=" + to + " by=" + transfer.by() + " name=" + name + " from=" + from);
        if (transfer.into() != null) {
            audit.record("PERSON_MERGED sheet=" + to + " by=" + transfer.by() + " from=" + name + " into="
                    + transfer.into().name());
        }
        rollup.record(to, transfer.by(), transfer.toBefore());
    }
}
